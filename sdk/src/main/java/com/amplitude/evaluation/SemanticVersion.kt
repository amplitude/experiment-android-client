package com.amplitude.experiment.evaluation

// major and minor should be non-negative numbers separated by a dot
private const val MAJOR_MINOR_REGEX = "(\\d+)\\.(\\d+)"

// patch should be a non-negative number
private const val PATCH_REGEX = "(\\d+)"

// prerelease is optional. If provided, it should be a hyphen followed by a
// series of dot separated identifiers where an identifer can contain anything in [-0-9a-zA-Z]
private const val PRERELEASE_REGEX = "(-(([-\\w]+\\.?)*))?"

// version pattern should be major.minor(.patchAndPreRelease) where .patchAndPreRelease is optional
private const val VERSION_PATTERN = "$MAJOR_MINOR_REGEX(\\.$PATCH_REGEX$PRERELEASE_REGEX)?$"

/**
 * Implementation of Semantic version specification as per the spec in
 * https://semver.org/#semantic-versioning-specification-semver
 *
 * Some important things to call out:
 *
 * - Major, minor, patch should not contain leading 0s and should increment numerically.
 * If leading 0s are specified, the information will be lost as we cast it to integer.
 * - Prerelease tags are optional and if provided they are considered as strings for comparison.
 * - Version with Prerelease tags < Same version without prerelease tags
 */
internal data class SemanticVersion(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val preRelease: String? = null
) : Comparable<SemanticVersion> {

    companion object {

        fun parse(version: String?): SemanticVersion? {
            if (version == null) {
                return null
            }
            val matchGroup = Regex(VERSION_PATTERN).matchEntire(version)?.groupValues ?: return null
            val major = matchGroup[1].toIntOrNull() ?: return null
            val minor = matchGroup[2].toIntOrNull() ?: return null
            val patch = matchGroup[4].toIntOrNull() ?: 0
            val preRelease = matchGroup[5].takeIf { it.isNotEmpty() }
            return SemanticVersion(major, minor, patch, preRelease)
        }
    }

    override fun compareTo(other: SemanticVersion): Int {
        return when {
            major > other.major -> 1
            major < other.major -> -1
            minor > other.minor -> 1
            minor < other.minor -> -1
            patch > other.patch -> 1
            patch < other.patch -> -1
            preRelease != null && other.preRelease == null -> -1
            preRelease == null && other.preRelease != null -> 1
            preRelease != null && other.preRelease != null ->
                preRelease.compareTo(other.preRelease)

            else -> 0
        }
    }
}
