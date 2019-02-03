package ocd.concurrent.util;

public class VersionUtils
{
    public static boolean isModificationInProgess(final int version)
    {
        return (version & 1) != 0;
    }

    public static int startModification(final int version)
    {
        return version + 1;
    }

    public static int endModification(final int version)
    {
        return version + 1;
    }

    public static boolean isConsistent(final int versionStart, final int versionEnd)
    {
        return versionStart == (versionEnd & -2);
    }
}
