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
    private static final int[] T_REG_INDEX = {0, 1, 2, 3, 4, 5, 6};
    private static final int[] S_REG_INDEX = {7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18};
    private static final int NUM_REGISTERS = REGISTERS.length;

    private final Map<String, LocalDataLocation> varToLocation;
    private final ArrayList<LiveInterval> intervals;
    private final List<String> paramVarNames;
    private final Set<LiveInterval> activeSortedByEnd;
    private final Set<Integer> freeTRegisters;
    private final Set<Integer> freeSRegisters;
    private final Set<String> usedCalleeSavedRegisters;
    private int currentOffset = 0;

    public LinearScanAllocator(Set<LiveInterval> intervals, List<String> paramVarNames) {
        this.intervals = new ArrayList<LiveInterval>(intervals);
        this.paramVarNames = paramVarNames;
        this.intervals.sort(Comparator.comparingInt(LiveInterval::getStart));
        // System.out.println("LinearScanAllocator initialized with intervals: " + this.intervals);
        this.varToLocation = new HashMap<>();
        this.activeSortedByEnd = new TreeSet<>((a, b) -> {
            int endCompare = Integer.compare(a.getEnd(), b.getEnd());
            if (endCompare != 0) return endCompare;
            int startCompare = Integer.compare(a.getStart(), b.getStart());
            if (startCompare != 0) return startCompare;
            return a.getVariable().compareTo(b.getVariable());
        });
        this.freeTRegisters = new LinkedHashSet<>();
        this.freeSRegisters = new LinkedHashSet<>();
        this.usedCalleeSavedRegisters = new HashSet<>();
        // 初始化t/s寄存器池
        for (int idx : T_REG_INDEX) freeTRegisters.add(idx);
        for (int idx : S_REG_INDEX) freeSRegisters.add(idx);
        // 1. 先为参数分配 a0~a7
        for (int i = 0; i < paramVarNames.size() && i < 8; i++) {
            varToLocation.put(paramVarNames.get(i), LocalDataLocation.createRegister("a" + i));
        }
        // 2. 其它变量再用原有分配逻辑
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

    private void allocateRegisters() {
        for (LiveInterval interval : intervals) {
            // 跳过已经分配了寄存器的变量（如参数变量）
            if (varToLocation.containsKey(interval.getVariable())) {
                continue;
            }
            expireOldIntervals(interval.getStart());
            int reg = -1;
            // 简单策略：活跃区间短优先t，长优先s
            if (interval.getEnd() - interval.getStart() <= 3 && !freeTRegisters.isEmpty()) {
                reg = freeTRegisters.iterator().next();
                freeTRegisters.remove(reg);
            } else if (!freeSRegisters.isEmpty()) {
                reg = freeSRegisters.iterator().next();
                freeSRegisters.remove(reg);
            }
            if (reg == -1) {
                spillAtInterval(interval);
            } else {
                interval.setRegister(reg);
                activeSortedByEnd.add(interval);
                String regName = REGISTERS[reg];
                if (CALLEE_SAVED_REGISTERS.contains(regName)) {
                    usedCalleeSavedRegisters.add(regName);
                }
            }
        }

        for (LiveInterval interval : intervals) {
            // 跳过已经分配了寄存器的变量（如参数变量）
            if (varToLocation.containsKey(interval.getVariable())) {
                continue;
            }
            LocalDataLocation location;
            if (interval.getRegister() == -1) {
                location = LocalDataLocation.createStack(currentOffset);
                currentOffset += 4;
            } else {
                location = LocalDataLocation.createRegister(REGISTERS[interval.getRegister()]);
            }
            varToLocation.put(interval.getVariable(), location);
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