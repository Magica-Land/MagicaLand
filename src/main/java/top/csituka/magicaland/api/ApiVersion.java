package top.csituka.magicaland.api;

/** Version of the public contract, independent of the mod release version. */
public final class ApiVersion {
    public static final int MAJOR = 1;
    public static final int MINOR = 0;

    private ApiVersion() {}

    public static boolean isCompatible(int requiredMajor, int minimumMinor) {
        return requiredMajor == MAJOR && minimumMinor >= 0 && minimumMinor <= MINOR;
    }

    public static void requireCompatible(int requiredMajor, int minimumMinor) {
        if (!isCompatible(requiredMajor, minimumMinor)) {
            throw new IllegalStateException("Magicaland appearance API " + requiredMajor + "." + minimumMinor
                    + " required; available " + MAJOR + "." + MINOR);
        }
    }
}
