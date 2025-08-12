package toyc.codegen.regalloc;

import java.util.*;


public class LinearScanAllocator implements RegisterAllocator {
    private static final String[] REGISTERS = {
            "t1", "t2", "t3", "t4", "t5", "t6",
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
            "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7"};
    private static final Set<String> CALLEE_SAVED_REGISTERS = Set.of(
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"
    );
    private static final int NUM_REGISTERS = REGISTERS.length;
    private final Map<String, LocalDataLocation> varToLocation;
    private final Set<LiveInterval> intervals;
    private final Set<LiveInterval> activeSortedByEnd;
    private final Set<Integer> freeRegisters;
    private final Set<String> usedCalleeSavedRegisters;
    private int currentOffset = 0;

    public LinearScanAllocator(Set<LiveInterval> intervals) {
        this.intervals = new TreeSet<>(Comparator.comparingInt(LiveInterval::getStart));
        this.intervals.addAll(intervals);
        this.varToLocation = new HashMap<>();
        this.activeSortedByEnd = new TreeSet<>((a, b) -> {
            int endCompare = Integer.compare(a.getEnd(), b.getEnd());
            if (endCompare != 0) {
                return endCompare;
            }
            // If end times are equal, compare by start times
            int startCompare = Integer.compare(a.getStart(), b.getStart());
            if (startCompare != 0) {
                return startCompare;
            }
            // If both end and start are equal, compare by variable names to ensure uniqueness
            return a.getVariable().compareTo(b.getVariable());
        });
        this.freeRegisters = new HashSet<>();
        this.usedCalleeSavedRegisters = new HashSet<>();
        // Initialize free registers
        for (int i = 0; i < NUM_REGISTERS; i++) {
            freeRegisters.add(i);
        }
        allocateRegisters();
    }

    @Override
    public LocalDataLocation allocate(String varName) {
        return varToLocation.get(varName);
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
            expireOldIntervals(interval.getStart());
            if (freeRegisters.isEmpty()) {
                spillAtInterval(interval);
            } else {
                int register = freeRegisters.iterator().next();
                freeRegisters.remove(register);
                interval.setRegister(register);
                activeSortedByEnd.add(interval);
                String regName = REGISTERS[register];
                if (CALLEE_SAVED_REGISTERS.contains(regName)) {
                    usedCalleeSavedRegisters.add(regName);
                }
            }
        }

        for (LiveInterval interval : intervals) {
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
            if (interval.getRegister() != -1) {  // Only add back non-spilled registers
                freeRegisters.add(interval.getRegister());
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