package toyc.util.formatter;

public enum SGR {
    UnknownSGR(-1),
    Reset(0),
    Bold(1),
    Dim(2),
    Underlined(4),
    Blink(5),
    Reverse(7),
    Hidden(8),
    ResetBold(21),
    ResetDim(22),
    ResetUnderlined(24),
    ResetBlink(25),
    ResetReverse(27),
    ResetHidden(28),
    // Foreground
    ResetFore(39),
    Black(30),
    Red(31),
    Green(32),
    Yellow(33),
    Blue(34),
    Magenta(35),
    Cyan(36),
    LightGray(37),
    DarkGray(90),
    LightRed(91),
    LightGreen(92),
    LightYellow(93),
    LightBlue(94),
    LightMagenta(95),
    LightCyan(96),
    White(97),
    // Background
    ResetBG(49),
    BlackBG(40),
    RedBG(41),
    GreenBG(42),
    YellowBG(43),
    BlueBG(44),
    MagentaBG(45),
    CyanBG(46),
    LightGrayBG(47),
    DarkGrayBG(100),
    LightRedBG(101),
    LightGreenBG(102),
    LightYellowBG(103),
    LightBlueBG(104),
    LightMagentaBG(105),
    LightCyanBG(106),
    WhiteBG(107);

    private final int code;

    SGR(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public String getAnsiCode() {
        return "\u001B[" + code + "m";
    }

    public static String combine(SGR... codes) {
        if (codes.length == 0) return "";
        StringBuilder sb = new StringBuilder("\u001B[");
        for (int i = 0; i < codes.length; i++) {
            if (i > 0) sb.append(";");
            sb.append(codes[i].getCode());
        }
        sb.append("m");
        return sb.toString();
    }

    public static String addUnderline(String baseStyle) {
        return baseStyle.substring(0, baseStyle.length() - 1) +
                ";" + SGR.Underlined.getCode() + "m";
    }
}
