package com.lockr.app

/** Bound dimensions before allocating pixels; EXIF rotations swap the target axes. */
internal fun imageSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int, orientation: Int = 1): Int {
    require(maxWidth > 0 && maxHeight > 0)
    require(width in 1..32_768 && height in 1..32_768 && width.toLong() * height <= 200_000_000) {
        "Choose an image smaller than 200 megapixels."
    }
    val rotated = orientation in 5..8
    val targetWidth = if (rotated) maxHeight else maxWidth
    val targetHeight = if (rotated) maxWidth else maxHeight
    var sample = 1
    while ((width + sample - 1) / sample > targetWidth || (height + sample - 1) / sample > targetHeight) sample *= 2
    return sample
}
