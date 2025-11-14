package net.shiroha233.roadweaver.structures.api;

public final class BlendProfile {
    private final FoundationType foundation;
    private final int ringInner;
    private final int ringOuter;
    private final int cutFillBudget;
    private final String kernel;
    private final boolean fellTree;
    private final String waterPolicy;

    public BlendProfile(FoundationType foundation, int ringInner, int ringOuter, int cutFillBudget, String kernel, boolean fellTree, String waterPolicy) {
        this.foundation = foundation;
        this.ringInner = Math.max(0, ringInner);
        this.ringOuter = Math.max(this.ringInner, ringOuter);
        this.cutFillBudget = Math.max(0, cutFillBudget);
        this.kernel = kernel;
        this.fellTree = fellTree;
        this.waterPolicy = waterPolicy;
    }

    public static BlendProfile platformDefault() {
        return new BlendProfile(FoundationType.PLATFORM, 2, 4, 2048, "quadratic", false, "replace");
    }

    public FoundationType foundation() { return foundation; }
    public int ringInner() { return ringInner; }
    public int ringOuter() { return ringOuter; }
    public int cutFillBudget() { return cutFillBudget; }
    public String kernel() { return kernel; }
    public boolean fellTree() { return fellTree; }
    public String waterPolicy() { return waterPolicy; }
}
