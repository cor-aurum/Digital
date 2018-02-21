package de.neemann.digital.draw.graphics.text.formatter;

import de.neemann.digital.draw.graphics.text.text.*;
import de.neemann.digital.draw.graphics.text.text.Character;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 * Formatter to draw a text on a {@link Graphics2D} instance.
 */
public final class GraphicsFormatter {

    private GraphicsFormatter() {
    }

    /**
     * Draws the given text.
     *
     * @param gr       the {@link Graphics2D} instance
     * @param x        the x position
     * @param y        the y position
     * @param fragment the text fragment
     */
    public static void draw(Graphics2D gr, int x, int y, Fragment fragment) {
        Font font = gr.getFont();
        Stroke stroke = gr.getStroke();
        fragment.draw(gr, x, y);
        gr.setFont(font);
        gr.setStroke(stroke);
    }


    /**
     * Creates the text fragments
     *
     * @param gr   the {@link Graphics2D} instance
     * @param text the text
     * @return the text fragment
     * @throws FormatterException FormatterException
     */
    public static Fragment createFragment(Graphics2D gr, Text text) throws FormatterException {
        return createFragment((fragment, font, str) -> {
            final FontMetrics metrics = gr.getFontMetrics(font);
            Rectangle2D rec = metrics.getStringBounds(str, gr);
            fragment.set((int) rec.getWidth(), (int) rec.getHeight(), metrics.getDescent());
        }, gr.getFont(), text);
    }

    /**
     * Creates the text fragments
     *
     * @param sizer the sizer instance
     * @param font  the font
     * @param text  the text
     * @return the fragment
     * @throws FormatterException FormatterException
     */
    public static Fragment createFragment(FontSizer sizer, Font font, Text text) throws FormatterException {
        if (text instanceof Simple) {
            return new TextFragment(sizer, font, ((Simple) text).getText());
        } else if (text instanceof Character) {
            return new TextFragment(sizer, font, "" + ((Character) text).getChar());
        } else if (text instanceof Sentence) {
            Sentence s = (Sentence) text;
            SentenceFragment sf = new SentenceFragment();
            int x = 0;
            for (Text t : s)
                if (t instanceof Blank)
                    x += font.getSize() / 2;
                else {
                    final Fragment f = createFragment(sizer, font, t);
                    f.x = x;
                    x += f.dx;
                    sf.add(f);
                }
            sf.dx = x;
            return sf.setUp();
        } else if (text instanceof Index) {
            Index i = (Index) text;
            Fragment var = createFragment(sizer, font, i.getVar());
            Font f = font.deriveFont(font.getSize() / 1.4f);
            Fragment superScript = i.getSuperScript() == null ? null : createFragment(sizer, f, i.getSuperScript());
            Fragment subScript = i.getSubScript() == null ? null : createFragment(sizer, f, i.getSubScript());
            return new IndexFragment(var, superScript, subScript);
        } else if (text instanceof Decorate) {
            Decorate d = (Decorate) text;
            switch (d.getStyle()) {
                case MATH:
                    return createFragment(sizer, font.deriveFont(Font.ITALIC), d.getContent());
                case OVERLINE:
                    return new OverlineFragment(createFragment(sizer, font, d.getContent()), font.getSize());
                default:
                    return createFragment(sizer, font, d.getContent());
            }
        } else
            throw new FormatterException("unknown text element " + text.getClass().getSimpleName() + ", " + text);
    }

    /**
     * Exception which indicates a formatter exception
     */
    public static final class FormatterException extends Exception {
        FormatterException(String message) {
            super(message);
        }
    }

    /**
     * The base class of all text fragments.
     */
    public static abstract class Fragment {
        //CHECKSTYLE.OFF: VisibilityModifier
        protected int x;
        protected int y;
        protected int dx;
        protected int dy;
        protected int base;
        //CHECKSTYLE.ON: VisibilityModifier

        private Fragment() {
        }

        /**
         * Sets the size of this fragment
         *
         * @param dx   width
         * @param dy   height
         * @param base base line
         */
        public void set(int dx, int dy, int base) {
            this.dx = dx;
            this.dy = dy;
            this.base = base;
        }

        void draw(Graphics2D gr, int xOfs, int yOfs) {
//            gr.setStroke(new BasicStroke());
//            gr.drawRect(xOfs + x, yOfs + y + base - dy, dx, dy);
//            gr.drawLine(xOfs + x, yOfs + y, xOfs + x + dx, yOfs + y);
        }

        /**
         * @return the width of this fragment
         */
        public int getWidth() {
            return dx + dy / 10;
        }
    }

    final static class TextFragment extends Fragment {
        private final String text;
        private final Font font;

        private TextFragment(FontSizer sizer, Font font, String text) {
            this.font = font;
            this.text = text;
            sizer.setSizeTo(this, font, text);
        }

        @Override
        void draw(Graphics2D gr, int xOfs, int yOfs) {
            super.draw(gr, xOfs, yOfs);
            gr.setFont(font);
            gr.drawString(text, x + xOfs, y + yOfs);
        }
    }

    private final static class SentenceFragment extends Fragment {

        private ArrayList<Fragment> fragments;

        private SentenceFragment() {
            this.fragments = new ArrayList<>();
        }

        private void add(Fragment fragment) {
            fragments.add(fragment);
        }

        @Override
        void draw(Graphics2D gr, int xOfs, int yOfs) {
            super.draw(gr, xOfs, yOfs);
            for (Fragment f : fragments)
                f.draw(gr, x + xOfs, y + yOfs);
        }

        public Fragment setUp() {
            int maxBase = 0;
            int maxAscent = 0;
            for (Fragment f : fragments) {
                if (maxBase < f.base)
                    maxBase = f.base;
                if (maxAscent < f.dy - f.base)
                    maxAscent = f.dy - f.base;
            }
            dy = maxBase + maxAscent;
            base = maxBase;
            return this;
        }
    }

    private final static class IndexFragment extends Fragment {
        private final Fragment var;
        private final Fragment superScript;
        private final Fragment subScript;

        private IndexFragment(Fragment var, Fragment superScript, Fragment subScript) {
            this.var = var;
            this.superScript = superScript;
            this.subScript = subScript;

            if (subScript != null && superScript != null)
                dx = var.dx + Math.max(subScript.dx, superScript.dx);
            else if (subScript != null)
                dx = var.dx + subScript.dx;
            else if (superScript != null)
                dx = var.dx + superScript.dx;
            else
                dx = var.dx;

            dy = var.dy;

            int delta = var.dy / 3;
            int ofs = var.dy / 8;
            if (superScript != null) {
                superScript.x = var.dx;
                superScript.y = -delta - ofs;

                int h = -superScript.y + superScript.dy - superScript.base;
                if (h > var.dy - var.base)
                    dy += h - (var.dy - var.base);
            }
            if (subScript != null) {
                subScript.x = var.dx;
                subScript.y = +delta - ofs;

                int b = subScript.y + subScript.base;
                if (b > var.base) {
                    base = b;
                    dy += b - var.base;
                } else
                    base = var.base;
            }
        }

        @Override
        void draw(Graphics2D gr, int xOfs, int yOfs) {
            super.draw(gr, xOfs, yOfs);
            var.draw(gr, xOfs + x, yOfs + y);
            if (superScript != null)
                superScript.draw(gr, xOfs + x, yOfs + y);
            if (subScript != null)
                subScript.draw(gr, xOfs + x, yOfs + y);
        }
    }

    private final static class OverlineFragment extends Fragment {
        private final Fragment fragment;
        private final int indent;
        private final float fontSize;

        private OverlineFragment(Fragment fragment, float fontSize) {
            this.fragment = fragment;
            this.fontSize = fontSize;
            this.dx = fragment.dx;
            this.dy = fragment.dy;
            this.base = fragment.base;
            this.indent = dx < fontSize / 2 ? 0 : (int) fontSize / 10;
        }

        @Override
        void draw(Graphics2D gr, int xOfs, int yOfs) {
            super.draw(gr, xOfs, yOfs);
            fragment.draw(gr, xOfs + x, yOfs + y);
            int yy = yOfs + y - dy + base;
            gr.setStroke(new BasicStroke(fontSize / 10f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            gr.drawLine(xOfs + x + indent, yy, xOfs + x + dx - indent / 2, yy);
        }
    }

    /**
     * Used to determine the size of a string
     */
    public interface FontSizer {
        /**
         * Must set the size of the given fragment by calling the {@link Fragment#set(int, int, int)} method.
         *
         * @param fragment fragment which size is requested
         * @param font     the used font
         * @param str      the string to measure
         */
        void setSizeTo(Fragment fragment, Font font, String str);
    }
}
