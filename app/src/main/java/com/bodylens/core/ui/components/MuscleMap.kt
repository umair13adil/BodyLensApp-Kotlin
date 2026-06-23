package com.bodylens.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.bodylens.domain.model.Muscle

/**
 * A lightweight, app-styled anatomy map. Front and back figures are drawn with the
 * Compose [Canvas] from vector polygons defined in a normalised 100×220 design space,
 * so there are no raster assets to license, ship, or scale — the whole map is a few KB
 * of geometry that stays crisp at every density.
 *
 * Muscles named by an [Exercise]'s [Muscle] strings are highlighted by colour: solid
 * accent for [primaryMuscles] (trained directly) and a translucent accent for
 * [secondaryMuscles] (assisting). To extend the map, add a [MuscleRegion] for the muscle
 * in [muscleRegions] — nothing else needs to change.
 */
@Composable
fun MuscleMap(
    primaryMuscles: Set<Muscle>,
    secondaryMuscles: Set<Muscle>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = MusclePalette(
        base = scheme.surfaceVariant,
        idleFill = scheme.onSurface.copy(alpha = 0.05f),
        idleOutline = scheme.onSurface.copy(alpha = 0.18f),
        primary = scheme.primary,
        secondary = scheme.primary.copy(alpha = 0.40f),
    )
    // A muscle counts as primary if it is primary for any contributing exercise; only
    // muscles that are *never* primary fall through to the secondary highlight.
    val secondaryOnly = secondaryMuscles - primaryMuscles

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FigureColumn("Front", BodyView.FRONT, primaryMuscles, secondaryOnly, palette, Modifier.weight(1f))
            FigureColumn("Back", BodyView.BACK, primaryMuscles, secondaryOnly, palette, Modifier.weight(1f))
        }
        if (showLegend) {
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendSwatch(palette.primary, "Primary")
                LegendSwatch(palette.secondary, "Secondary")
            }
        }
    }
}

@Composable
private fun FigureColumn(
    label: String,
    view: BodyView,
    primary: Set<Muscle>,
    secondaryOnly: Set<Muscle>,
    palette: MusclePalette,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(DESIGN_WIDTH / DESIGN_HEIGHT),
        ) {
            drawFigure(view, primary, secondaryOnly, palette)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------

private const val DESIGN_WIDTH = 100f
private const val DESIGN_HEIGHT = 220f

private enum class BodyView { FRONT, BACK }

private data class MusclePalette(
    val base: Color,
    val idleFill: Color,
    val idleOutline: Color,
    val primary: Color,
    val secondary: Color,
)

private fun DrawScope.drawFigure(
    view: BodyView,
    primary: Set<Muscle>,
    secondaryOnly: Set<Muscle>,
    palette: MusclePalette,
) {
    // Preserve aspect ratio, centre the figure in whatever box we were given.
    val scale = minOf(size.width / DESIGN_WIDTH, size.height / DESIGN_HEIGHT)
    val offsetX = (size.width - DESIGN_WIDTH * scale) / 2f
    val offsetY = (size.height - DESIGN_HEIGHT * scale) / 2f
    fun map(p: Offset) = Offset(offsetX + p.x * scale, offsetY + p.y * scale)

    // 1. The neutral body — head, limbs, torso — as a backdrop the muscles sit on.
    bodyBaseParts(view).forEach { part ->
        when (part) {
            is BodyPart.Blob -> drawPath(part.points.map(::map).toSmoothPath(), palette.base)
            is BodyPart.Circle -> drawCircle(palette.base, part.radius * scale, map(part.center))
        }
    }

    // 2. Muscle regions, drawn idle first then highlighted on top so accents win overlaps.
    val regions = muscleRegions.filter { it.view == view }
    val ordered = regions.sortedBy { region ->
        when {
            region.muscle in primary -> 2
            region.muscle in secondaryOnly -> 1
            else -> 0
        }
    }
    ordered.forEach { region ->
        val fill = when {
            region.muscle in primary -> palette.primary
            region.muscle in secondaryOnly -> palette.secondary
            else -> palette.idleFill
        }
        region.polygons.forEach { polygon ->
            val path = polygon.map(::map).toSmoothPath()
            drawPath(path, fill)
            drawPath(path, palette.idleOutline, style = Stroke(width = scale))
        }
    }
}

/** Build a closed, smoothly-rounded path through [this] polygon's vertices. */
private fun List<Offset>.toSmoothPath(): Path {
    val path = Path()
    if (size < 3) return path
    fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val start = mid(this[size - 1], this[0])
    path.moveTo(start.x, start.y)
    for (i in indices) {
        val current = this[i]
        val next = this[(i + 1) % size]
        val end = mid(current, next)
        path.quadraticBezierTo(current.x, current.y, end.x, end.y)
    }
    path.close()
    return path
}

// ---------------------------------------------------------------------------
// Geometry (normalised 100×220 design space; centre line at x = 50)
// ---------------------------------------------------------------------------

private sealed interface BodyPart {
    data class Blob(val points: List<Offset>) : BodyPart
    data class Circle(val center: Offset, val radius: Float) : BodyPart
}

private data class MuscleRegion(
    val muscle: Muscle,
    val view: BodyView,
    val polygons: List<List<Offset>>,
)

private fun pts(vararg p: Pair<Float, Float>): List<Offset> = p.map { Offset(it.first, it.second) }

/** Mirror a right-side polygon across the centre line to produce its left-side twin. */
private fun mirror(points: List<Offset>): List<Offset> = points.map { Offset(DESIGN_WIDTH - it.x, it.y) }

/** A symmetric muscle: same shape on both sides of the body. */
private fun symmetric(muscle: Muscle, view: BodyView, right: List<Offset>) =
    MuscleRegion(muscle, view, listOf(right, mirror(right)))

/** A single central muscle straddling the centre line. */
private fun central(muscle: Muscle, view: BodyView, shape: List<Offset>) =
    MuscleRegion(muscle, view, listOf(shape))

private fun bodyBaseParts(view: BodyView): List<BodyPart> {
    val rightArm = pts(68f to 38f, 82f to 44f, 86f to 96f, 84f to 122f, 76f to 122f, 74f to 96f, 70f to 60f)
    val rightLeg = pts(51f to 124f, 69f to 122f, 67f to 168f, 63f to 200f, 57f to 213f, 53f to 200f, 51f to 168f)
    return listOf(
        BodyPart.Circle(Offset(50f, 16f), 11f),                                    // head
        BodyPart.Blob(pts(44f to 26f, 56f to 26f, 56f to 35f, 44f to 35f)),        // neck
        BodyPart.Blob(pts(32f to 35f, 68f to 35f, 70f to 72f, 66f to 106f, 34f to 106f, 30f to 72f)), // torso
        BodyPart.Blob(rightArm), BodyPart.Blob(mirror(rightArm)),                  // arms
        BodyPart.Blob(pts(34f to 104f, 66f to 104f, 64f to 124f, 50f to 130f, 36f to 124f)), // pelvis
        BodyPart.Blob(rightLeg), BodyPart.Blob(mirror(rightLeg)),                  // legs
        BodyPart.Circle(Offset(84f, 124f), 5f), BodyPart.Circle(Offset(16f, 124f), 5f), // hands
        BodyPart.Circle(Offset(58f, 214f), 5f), BodyPart.Circle(Offset(42f, 214f), 5f), // feet
    )
}

private val muscleRegions: List<MuscleRegion> = listOf(
    // ---- Front ----
    symmetric(Muscle.TRAPS, BodyView.FRONT, pts(50f to 34f, 60f to 36f, 58f to 43f, 50f to 42f)),
    symmetric(Muscle.SHOULDERS, BodyView.FRONT, pts(66f to 38f, 80f to 44f, 80f to 55f, 70f to 55f, 67f to 46f)),
    symmetric(Muscle.CHEST, BodyView.FRONT, pts(51f to 46f, 67f to 48f, 69f to 60f, 60f to 69f, 51f to 66f)),
    symmetric(Muscle.BICEPS, BodyView.FRONT, pts(70f to 58f, 82f to 56f, 84f to 80f, 76f to 82f, 72f to 66f)),
    symmetric(Muscle.FOREARMS, BodyView.FRONT, pts(75f to 84f, 85f to 84f, 86f to 112f, 78f to 116f, 74f to 90f)),
    central(Muscle.ABDOMINALS, BodyView.FRONT, pts(40f to 70f, 60f to 70f, 61f to 90f, 59f to 104f, 50f to 112f, 41f to 104f, 39f to 90f)),
    symmetric(Muscle.QUADRICEPS, BodyView.FRONT, pts(52f to 126f, 68f to 124f, 66f to 158f, 58f to 166f, 52f to 150f)),
    symmetric(Muscle.ADDUCTORS, BodyView.FRONT, pts(51f to 128f, 58f to 128f, 56f to 150f, 51f to 148f)),
    central(Muscle.NECK, BodyView.FRONT, pts(45f to 27f, 55f to 27f, 54f to 34f, 46f to 34f)),

    // ---- Back ----
    central(Muscle.TRAPS, BodyView.BACK, pts(40f to 35f, 60f to 35f, 64f to 50f, 50f to 64f, 36f to 50f)),
    symmetric(Muscle.SHOULDERS, BodyView.BACK, pts(64f to 40f, 78f to 46f, 78f to 56f, 68f to 56f, 65f to 47f)),
    symmetric(Muscle.TRICEPS, BodyView.BACK, pts(70f to 58f, 82f to 57f, 84f to 80f, 76f to 82f, 72f to 66f)),
    symmetric(Muscle.FOREARMS, BodyView.BACK, pts(75f to 84f, 85f to 84f, 86f to 112f, 78f to 116f, 74f to 90f)),
    central(Muscle.MIDDLE_BACK, BodyView.BACK, pts(40f to 64f, 60f to 64f, 60f to 78f, 40f to 78f)),
    symmetric(Muscle.LATS, BodyView.BACK, pts(60f to 66f, 70f to 68f, 68f to 92f, 57f to 96f, 57f to 74f)),
    central(Muscle.LOWER_BACK, BodyView.BACK, pts(41f to 90f, 59f to 90f, 58f to 106f, 42f to 106f)),
    symmetric(Muscle.GLUTES, BodyView.BACK, pts(50f to 108f, 67f to 108f, 66f to 128f, 52f to 130f)),
    symmetric(Muscle.ABDUCTORS, BodyView.BACK, pts(66f to 110f, 72f to 116f, 70f to 130f, 65f to 128f)),
    symmetric(Muscle.HAMSTRINGS, BodyView.BACK, pts(52f to 132f, 67f to 130f, 65f to 162f, 56f to 168f, 52f to 150f)),
    symmetric(Muscle.CALVES, BodyView.BACK, pts(54f to 172f, 66f to 170f, 64f to 198f, 57f to 206f, 53f to 186f)),
    central(Muscle.NECK, BodyView.BACK, pts(45f to 28f, 55f to 28f, 54f to 35f, 46f to 35f)),
)
