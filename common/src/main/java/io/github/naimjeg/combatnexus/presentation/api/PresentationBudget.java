package io.github.naimjeg.combatnexus.presentation.api;

/** Presentation work may be refused; authoritative battle capture never uses this budget. */
public final class PresentationBudget {
    private int entries, bytes;
    private final int maximumEntries, maximumBytes;
    public PresentationBudget(int maximumEntries, int maximumBytes) {
        this.maximumEntries = maximumEntries; this.maximumBytes = maximumBytes;
    }
    public void entry(int count) {
        if (count < 0 || count > maximumEntries - entries) throw new Exceeded();
        entries += count;
    }
    public void encoded(String text) {
        if (text.length() > 4096) throw new Exceeded();
        int size = text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (size > maximumBytes - bytes) throw new Exceeded();
        bytes += size;
    }
    public static final class Exceeded extends RuntimeException {
        public Exceeded() { super("Presentation budget exceeded"); }
    }
}
