package zm.co.tbz.goldenleaf.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * On-device document scanning pipeline: edge detection -> auto-crop ->
 * perspective deskew -> Coons-patch dewarp. Operates on a single still
 * [Bitmap] (no live camera analyzer); designed to never produce a worse
 * result than the original image - any failure stage falls back to the
 * previous, simpler stage.
 */
object DocumentScanner {

    data class DocumentBounds(
        val corners: List<Point>, // ordered: topLeft, topRight, bottomRight, bottomLeft (original image scale)
        val contour: List<Point>, // full contour points (original image scale)
    )

    private data class BoundaryEdges(
        val top: Polyline,
        val right: Polyline,
        val bottom: Polyline,
        val left: Polyline,
        val tl: Point,
        val tr: Point,
        val br: Point,
        val bl: Point,
    )

    private const val DOWNSCALE_TARGET = 800.0

    /** Finds the document's 4-corner quadrilateral, or null if none was found. */
    fun detectDocument(bitmap: Bitmap): DocumentBounds? {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

        val scale = (DOWNSCALE_TARGET / maxOf(srcMat.width(), srcMat.height())).coerceAtMost(1.0)
        val workMat = Mat()
        Imgproc.resize(srcMat, workMat, Size(srcMat.width() * scale, srcMat.height() * scale))

        val gray = Mat()
        Imgproc.cvtColor(workMat, gray, Imgproc.COLOR_RGB2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)
        val dilateKernel = Mat()
        Imgproc.dilate(edges, edges, dilateKernel, Point(-1.0, -1.0), 2)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        val workArea = workMat.width().toDouble() * workMat.height().toDouble()
        var bestQuad: List<Point>? = null
        var bestContour: List<Point>? = null
        var bestArea = 0.0

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < workArea * 0.2 || area <= bestArea) continue
            val contour2f = MatOfPoint2f(*contour.toArray())
            val perimeter = Imgproc.arcLength(contour2f, true)
            val approx2f = MatOfPoint2f()
            Imgproc.approxPolyDP(contour2f, approx2f, 0.02 * perimeter, true)
            val approxPoints = approx2f.toArray()
            if (approxPoints.size == 4 && Imgproc.isContourConvex(MatOfPoint(*approxPoints))) {
                bestQuad = approxPoints.toList()
                bestContour = contour.toArray().toList()
                bestArea = area
            }
            contour2f.release()
            approx2f.release()
        }

        srcMat.release()
        workMat.release()
        gray.release()
        edges.release()
        dilateKernel.release()
        hierarchy.release()
        contours.forEach { it.release() }

        val quad = bestQuad ?: return null
        val contourPoints = bestContour ?: return null

        val invScale = 1.0 / scale
        val orderedCorners = orderCorners(quad.map { Point(it.x * invScale, it.y * invScale) })
        val scaledContour = contourPoints.map { Point(it.x * invScale, it.y * invScale) }

        return DocumentBounds(orderedCorners, scaledContour)
    }

    /** Orders 4 arbitrary corners into [topLeft, topRight, bottomRight, bottomLeft] using sum/diff. */
    private fun orderCorners(points: List<Point>): List<Point> {
        val sums = points.map { it.x + it.y }
        val diffs = points.map { it.x - it.y }
        val topLeft = points[sums.indices.minBy { sums[it] }]
        val bottomRight = points[sums.indices.maxBy { sums[it] }]
        val topRight = points[diffs.indices.maxBy { diffs[it] }]
        val bottomLeft = points[diffs.indices.minBy { diffs[it] }]
        return listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    private fun distance(a: Point, b: Point): Double = hypot(a.x - b.x, a.y - b.y)

    /** Crops and perspective-corrects (deskews) the document into a flat top-down rectangle. */
    fun cropAndDeskew(bitmap: Bitmap, bounds: DocumentBounds): Bitmap {
        val (tl, tr, br, bl) = bounds.corners
        val widthTop = distance(tl, tr)
        val widthBottom = distance(bl, br)
        val heightLeft = distance(tl, bl)
        val heightRight = distance(tr, br)
        val outWidth = max(widthTop, widthBottom).roundToInt().coerceAtLeast(1)
        val outHeight = max(heightLeft, heightRight).roundToInt().coerceAtLeast(1)

        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

        val srcPoints = MatOfPoint2f(tl, tr, br, bl)
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point((outWidth - 1).toDouble(), 0.0),
            Point((outWidth - 1).toDouble(), (outHeight - 1).toDouble()),
            Point(0.0, (outHeight - 1).toDouble()),
        )
        val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val dstMat = Mat()
        Imgproc.warpPerspective(srcMat, dstMat, transform, Size(outWidth.toDouble(), outHeight.toDouble()))

        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dstMat, result)

        srcMat.release()
        dstMat.release()
        transform.release()
        srcPoints.release()
        dstPoints.release()
        return result
    }

    /**
     * Flattens bends/curves in the paper (e.g. a folded page) via a Coons-patch
     * boundary-curve remap, then deskews the result. Falls back to plain
     * [cropAndDeskew] if the contour's boundary curves can't be extracted.
     */
    fun dewarp(bitmap: Bitmap, bounds: DocumentBounds): Bitmap {
        val edges = extractBoundaryEdges(bounds) ?: return cropAndDeskew(bitmap, bounds)

        val widthTop = distance(bounds.corners[0], bounds.corners[1])
        val widthBottom = distance(bounds.corners[3], bounds.corners[2])
        val heightLeft = distance(bounds.corners[0], bounds.corners[3])
        val heightRight = distance(bounds.corners[1], bounds.corners[2])
        val outWidth = max(widthTop, widthBottom).roundToInt().coerceAtLeast(1)
        val outHeight = max(heightLeft, heightRight).roundToInt().coerceAtLeast(1)

        val gridCols = 24
        val gridRows = 32
        val mapXSmall = Mat(gridRows, gridCols, CvType.CV_32F)
        val mapYSmall = Mat(gridRows, gridCols, CvType.CV_32F)
        for (row in 0 until gridRows) {
            val t = row.toDouble() / (gridRows - 1)
            for (col in 0 until gridCols) {
                val s = col.toDouble() / (gridCols - 1)
                val p = coonsPoint(edges, s, t)
                mapXSmall.put(row, col, floatArrayOf(p.x.toFloat()))
                mapYSmall.put(row, col, floatArrayOf(p.y.toFloat()))
            }
        }

        val mapX = Mat()
        val mapY = Mat()
        Imgproc.resize(mapXSmall, mapX, Size(outWidth.toDouble(), outHeight.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)
        Imgproc.resize(mapYSmall, mapY, Size(outWidth.toDouble(), outHeight.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)

        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

        val dstMat = Mat()
        Imgproc.remap(srcMat, dstMat, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE)

        val result = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(dstMat, result)

        mapXSmall.release()
        mapYSmall.release()
        mapX.release()
        mapY.release()
        srcMat.release()
        dstMat.release()
        return result
    }

    /**
     * Runs the full pipeline (detect -> dewarp, falling back to crop+deskew, falling
     * back to the original bitmap). Safe to call on any picked/captured photo - never
     * throws and never returns a worse result than the input.
     */
    fun scanDocument(bitmap: Bitmap): Bitmap {
        val bounds = detectDocument(bitmap) ?: return bitmap
        return runCatching { dewarp(bitmap, bounds) }.getOrElse { cropAndDeskew(bitmap, bounds) }
    }

    private fun extractBoundaryEdges(bounds: DocumentBounds): BoundaryEdges? {
        val contour = bounds.contour
        if (contour.size < 8) return null
        val (tl, tr, br, bl) = bounds.corners

        fun nearestIndex(p: Point): Int =
            contour.indices.minByOrNull { distance(contour[it], p) } ?: 0

        val tlIdx = nearestIndex(tl)
        val trIdx = nearestIndex(tr)
        val brIdx = nearestIndex(br)
        val blIdx = nearestIndex(bl)

        val labeled = listOf("tl" to tlIdx, "tr" to trIdx, "br" to brIdx, "bl" to blIdx)
            .sortedBy { it.second }

        if (labeled.map { it.second }.toSet().size < 4) return null

        fun sliceForward(fromIdx: Int, toIdx: Int): List<Point> {
            val n = contour.size
            return if (fromIdx <= toIdx) {
                contour.subList(fromIdx, toIdx + 1)
            } else {
                contour.subList(fromIdx, n) + contour.subList(0, toIdx + 1)
            }
        }

        val arcs = (0 until 4).map { i ->
            val (labelA, idxA) = labeled[i]
            val (labelB, idxB) = labeled[(i + 1) % 4]
            Triple(labelA, labelB, sliceForward(idxA, idxB))
        }

        fun findEdge(from: String, to: String): List<Point>? {
            for ((a, b, pts) in arcs) {
                if (a == from && b == to) return pts
                if (a == to && b == from) return pts.reversed()
            }
            return null
        }

        val topPts = findEdge("tl", "tr") ?: return null
        val rightPts = findEdge("tr", "br") ?: return null
        val bottomPts = findEdge("bl", "br") ?: return null
        val leftPts = findEdge("tl", "bl") ?: return null

        return BoundaryEdges(
            top = Polyline(topPts),
            right = Polyline(rightPts),
            bottom = Polyline(bottomPts),
            left = Polyline(leftPts),
            tl = tl,
            tr = tr,
            br = br,
            bl = bl,
        )
    }

    /** Standard Coons-patch bilinear-blend formula over the 4 boundary curves. */
    private fun coonsPoint(edges: BoundaryEdges, s: Double, t: Double): Point {
        val top = edges.top.pointAt(s)
        val bottom = edges.bottom.pointAt(s)
        val left = edges.left.pointAt(t)
        val right = edges.right.pointAt(t)
        val x = (1 - t) * top.x + t * bottom.x + (1 - s) * left.x + s * right.x -
            ((1 - s) * (1 - t) * edges.tl.x + s * (1 - t) * edges.tr.x + (1 - s) * t * edges.bl.x + s * t * edges.br.x)
        val y = (1 - t) * top.y + t * bottom.y + (1 - s) * left.y + s * right.y -
            ((1 - s) * (1 - t) * edges.tl.y + s * (1 - t) * edges.tr.y + (1 - s) * t * edges.bl.y + s * t * edges.br.y)
        return Point(x, y)
    }

    /** Arc-length parameterized lookup along a polyline, for boundary curve sampling. */
    private class Polyline(rawPoints: List<Point>) {
        private val points: List<Point> = if (rawPoints.size >= 2) rawPoints else listOf(rawPoints.first(), rawPoints.first())
        private val cumulative: DoubleArray
        val length: Double

        init {
            val cum = DoubleArray(points.size)
            for (i in 1 until points.size) {
                cum[i] = cum[i - 1] + distance(points[i - 1], points[i])
            }
            cumulative = cum
            length = cum.last()
        }

        fun pointAt(t: Double): Point {
            if (length <= 0.0) return points.first()
            val target = t.coerceIn(0.0, 1.0) * length
            var idx = cumulative.indexOfLast { it <= target }
            if (idx < 0) idx = 0
            if (idx >= points.size - 1) return points.last()
            val segStart = cumulative[idx]
            val segLen = cumulative[idx + 1] - segStart
            val frac = if (segLen > 0) (target - segStart) / segLen else 0.0
            val a = points[idx]
            val b = points[idx + 1]
            return Point(a.x + (b.x - a.x) * frac, a.y + (b.y - a.y) * frac)
        }
    }
}

/** Loads/saves bitmaps for the document scanner into app-private storage as `file://` URIs. */
object ScannedImageStore {

    fun loadBitmap(context: Context, uri: Uri): Bitmap? =
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

    fun save(context: Context, bitmap: Bitmap, prefix: String): Uri {
        val dir = File(context.filesDir, "scans").apply { mkdirs() }
        val file = File(dir, "${prefix}_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file)
    }
}
