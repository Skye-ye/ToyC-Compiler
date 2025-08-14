package toyc.codegen;

public final class TargetArchitecture {

    public enum Architecture {
        X86_32("x86", 32, Endianness.LITTLE, "i386"),
        X86_64("x86_64", 64, Endianness.LITTLE, "amd64"),
        ARM32("arm", 32, Endianness.LITTLE, "armv7"),
        ARM64("arm64", 64, Endianness.LITTLE, "aarch64"),
        RISC_V_32("riscv32", 32, Endianness.LITTLE, "rv32"),
        RISC_V_64("riscv64", 64, Endianness.LITTLE, "rv64"),
        MIPS("mips", 32, Endianness.BIG, "mips32"),
        MIPS64("mips64", 64, Endianness.BIG);

        private final String name;
        private final int bitWidth;
        private final Endianness endianness;
        private final String alternateName;

        Architecture(String name, int bitWidth, Endianness endianness, String alternateName) {
            this.name = name;
            this.bitWidth = bitWidth;
            this.endianness = endianness;
            this.alternateName = alternateName;
        }

        Architecture(String name, int bitWidth, Endianness endianness) {
            this(name, bitWidth, endianness, null);
        }

        public String getName() { return name; }
        public int getBitWidth() { return bitWidth; }
        public Endianness getEndianness() { return endianness; }
        public String getAlternateName() { return alternateName; }

        public boolean is64Bit() { return bitWidth == 64; }
        public boolean is32Bit() { return bitWidth == 32; }
        public boolean isLittleEndian() { return endianness == Endianness.LITTLE; }
    }

    public enum Endianness {
        LITTLE, BIG
    }

    // Private constructor to prevent instantiation
    private TargetArchitecture() {}
}