/*
 * MIT License
 *
 * Copyright (c) 2022 MatrixEditor
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.ubnt.imageio; //@date 14.11.2022

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.apache.batik.transcoder.XMLAbstractTranscoder.*;
import static org.apache.batik.util.SVGConstants.SVG_NAMESPACE_URI;
import static org.apache.batik.util.SVGConstants.SVG_SVG_TAG;

/**
 * A simple utility class to wrap the SVG-2-Image conversion.
 * <p>
 * This class is mainly used to convert the SVG resource files to into
 * {@link BufferedImage}s.
 *
 * @since 0.0.1
 */
public final class SVGUtil {

    private SVGUtil() {}

    /**
     * Tries to convert an SVG file into an {@link BufferedImage}.
     *
     * @param file the resource path
     * @return the converted image
     * @throws IOException if an I/O error occurs
     */
    public static Image loadSVGFromFile(String file) throws IOException {
        return loadSVG(new File(file));
    }

    /**
     * Tries to convert an SVG file by its path into an {@link BufferedImage}.
     *
     * @param file the resource path
     * @return the converted image
     * @throws IOException if an I/O error occurs
     */
    public static Image loadSVGFromResource(String file) throws IOException {
        URL url = SVGUtil.class.getResource(file);
        return url != null ? loadSVG(url) : null;
    }

    /**
     * Tries to convert an SVG file into an {@link BufferedImage}.
     *
     * @param file the resource path
     * @return the converted image
     * @throws IOException if an I/O error occurs
     */
    public static Image loadSVG(File file) throws IOException {
        return loadSVG(file.toURI().toURL());
    }

    /**
     * Tries to convert an SVG file by the given {@link URL} into an
     * {@link BufferedImage}.
     *
     * @param url the resource path
     * @return the converted image
     * @throws IOException if an I/O error occurs
     */
    public static Image loadSVG(URL url) throws IOException {
        SvgTranscoder transcoder = new SvgTranscoder();
        transcoder.setTranscodingHints(getHints());
        try {
            TranscoderInput input = new TranscoderInput(url.openStream());
            transcoder.transcode(input, null);
        } catch (TranscoderException e) {
            throw new IOException("Error parsing SVG file " + url, e);
        }
        return transcoder.getImage();
    }

    /**
     * @return the transcoding hints for SVG images-
     */
    private static TranscodingHints getHints() {
        TranscodingHints hints = new TranscodingHints();
        hints.put(KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
        hints.put(KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVG_NAMESPACE_URI);
        hints.put(KEY_DOCUMENT_ELEMENT, SVG_SVG_TAG);
        return hints;
    }

    /**
     * The {@link ImageTranscoder} for basic SVG images.
     */
    private static class SvgTranscoder extends ImageTranscoder {

        /**
         * the image that will be crated.
         */
        private BufferedImage image = null;

        @Override
        public BufferedImage createImage(int width, int height) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return image;
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput out) {
        }

        public BufferedImage getImage() {
            return image;
        }
    }

}
