package toyc.codegen.regalloc;

import java.util.*;

public class LinearScanAllocator implements RegisterAllocator {
    private static final String[] REGISTERS = {
            "t0", "t1", "t2", "t3", "t4", "t5", "t6",           // 0-6
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", // 7-18
            "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7" // 19-26
    };
    private static final Set<String> CALLEE_SAVED_REGISTERS = Set.of(
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"
    );
    
    private final Set<String> usedCalleeSavedRegisters = new HashSet<>();
    private final Set<String> usedCallerSavedRegisters = new HashSet<>();

    private static final int[] T_REG_INDEX = {0, 1, 2, 3, 4, 5, 6};
    private static final int[] S_REG_INDEX = {7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
    private static final int NUM_REGISTERS = REGISTERS.length;

    private final Map<String, LocalDataLocation> varToLocation = new HashMap<>();
    private final ArrayList<LiveInterval> intervals;
    private final List<String> paramVarNames; // 形参名字，来自 IR
    private final Set<LiveInterval> activeSortedByEnd = new TreeSet<>((a,b)->{
        int c = Integer.compare(a.getEnd(), b.getEnd());
        if (c != 0) return c;
        c = Integer.compare(a.getStart(), b.getStart());
        if (c != 0) return c;
        return a.getVariable().compareTo(b.getVariable());
    });
    private final Set<Integer> freeTRegisters = new LinkedHashSet<>();
    private final Set<Integer> freeSRegisters = new LinkedHashSet<>();
    
    private int currentOffset = 0;

    public LinearScanAllocator(Set<LiveInterval> intervals, List<String> paramVarNames) {
        this.intervals = new ArrayList<>(intervals);
        this.paramVarNames = paramVarNames;
        this.intervals.sort(Comparator.comparingInt(LiveInterval::getStart));
        for (int idx : T_REG_INDEX) freeTRegisters.add(idx);
        for (int idx : S_REG_INDEX) freeSRegisters.add(idx);

        // 确保所有参数都有活跃区间
        for (String paramName : paramVarNames) {
            boolean hasInterval = false;
            for (LiveInterval interval : this.intervals) {
                if (interval.getVariable().equals(paramName)) {
                    hasInterval = true;
                    break;
                }
            }
            if (!hasInterval) {
                // 为参数添加一个从开始位置的"虚拟"活跃区间
                this.intervals.add(new LiveInterval(0, 1, paramName));
            }
        }

        // 不再为参数绑定 a0~a7，参数当作普通局部变量参与分配，但优先 s 寄存器
        allocateRegisters();
    }

    @Override
    public LocalDataLocation allocate(String varName) {
        // return varToLocation.get(varName);
        LocalDataLocation location = varToLocation.get(varName);
        if (location == null) {
            System.err.println("allocate() failed for var: " + varName + ", varToLocation keys: " + varToLocation.keySet());
        }
        return location;
    }

    @Override
    public int getStackSize() {
        return (currentOffset + 15) & ~15;
    }

    @Override
    public Set<String> getUsedCalleeSavedRegisters() {
        return Collections.unmodifiableSet(usedCalleeSavedRegisters);
    }

    @Override
    public Set<String> getUsedCallerSavedRegisters() {
        return Collections.unmodifiableSet(usedCallerSavedRegisters);
    }

    private void allocateRegisters() {
        for (LiveInterval interval : intervals) {
            // 跳过已经分配了寄存器的变量（如参数变量）
            if (varToLocation.containsKey(interval.getVariable())) {
                continue;
            }

            expireOldIntervals(interval.getStart());
            
            int reg = -1;
            boolean isParam = paramVarNames.contains(interval.getVariable());

            // 形参优先使用 s 寄存器（callee-saved），避免跨调用被 clobber
            if (isParam && !freeSRegisters.isEmpty()) {
                reg = freeSRegisters.iterator().next();
                freeSRegisters.remove(reg);
            } else {
                // 简单策略：短区间 -> t；否则 -> s
                if (interval.getEnd() - interval.getStart() <= 3 && !freeTRegisters.isEmpty()) {
                    reg = freeTRegisters.iterator().next();
                    freeTRegisters.remove(reg);
                } else if (!freeSRegisters.isEmpty()) {
                    reg = freeSRegisters.iterator().next();
                    freeSRegisters.remove(reg);
                }
            }

            if (reg == -1) {
                spillAtInterval(interval);
            } else {
                interval.setRegister(reg);
                activeSortedByEnd.add(interval);
                String regName = REGISTERS[reg];
                if (CALLEE_SAVED_REGISTERS.contains(regName)) {
                    usedCalleeSavedRegisters.add(regName);
                } else if (regName.startsWith("t")) {
                    usedCallerSavedRegisters.add(regName);
                }
            }
        }

        // 为所有 interval 生成最终位置
        for (LiveInterval interval : intervals) {
            if (varToLocation.containsKey(interval.getVariable())) continue;
            LocalDataLocation loc;
            if (interval.getRegister() == -1) {
                loc = LocalDataLocation.createStack(currentOffset);
                currentOffset += 4;
            } else {
                loc = LocalDataLocation.createRegister(REGISTERS[interval.getRegister()]);
            }
            varToLocation.put(interval.getVariable(), loc);
        }
    }

    private void expireOldIntervals(int startPoint) {
        Iterator<LiveInterval> iterator = activeSortedByEnd.iterator();
        while (iterator.hasNext()) {
            LiveInterval interval = iterator.next();
            if (interval.getEnd() >= startPoint) {
                return;
            }
            if (interval.getRegister() != -1) {
                int reg = interval.getRegister();
                if (Arrays.stream(T_REG_INDEX).anyMatch(i -> i == reg)) freeTRegisters.add(reg);
                else if (Arrays.stream(S_REG_INDEX).anyMatch(i -> i == reg)) freeSRegisters.add(reg);
            }
            iterator.remove();
        }
    }

    private void spillAtInterval(LiveInterval interval) {
        LiveInterval spill = activeSortedByEnd.stream()
                .max(Comparator.comparingInt(LiveInterval::getEnd))
                .orElse(null);
        assert spill != null;
        if (spill.getEnd() > interval.getEnd()) {
            interval.setRegister(spill.getRegister());
            activeSortedByEnd.remove(spill);
            activeSortedByEnd.add(interval);
            spill.setRegister(-1);
        } else {
            interval.setRegister(-1);
        }
    }
}