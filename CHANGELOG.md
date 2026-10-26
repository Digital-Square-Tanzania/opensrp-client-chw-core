
# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## 2.2.0-MOTHER-MENTOR-ALPHA-13-MOH-SNAPSHOT — 2026-10-26

### Changed

- Upgrade Mother Mentor to `0.1.10-SNAPSHOT` for the merged daily-visit,
  optional-comment, HEI identifier and infant-age fixes.
- Increment the Core library version code from 149 to 150.

### Fixed

- Open Mother Mentor client and location information using persisted client
  relationships and prepopulated registration details. Preserve the client's
  identifier, caregiver status and update event metadata without duplicate form
  launches or session-interrupting missing-record errors (CHW issue #1093).

## 2.2.0-MOTHER-MENTOR-ALPHA-12-MOH-SNAPSHOT — 2026-10-25

### Changed

- Upgrade HPS to `2.0.7-SNAPSHOT` for the two-row iCCHW client list, colored
  sex values, and profile opening from a client row tap.
- Preserve the latest FOLLOW-UP / UFUATILIAJI navigation section from Alpha 11.
- Increment the Core library version code from 148 to 149.

## 2.2.0-MOTHER-MENTOR-ALPHA-11-MOH-SNAPSHOT — 2026-10-25

### Changed

- Add an opt-in, always-visible FOLLOW-UP / UFUATILIAJI navigation section
  below Other Modules, preserving role visibility, register counts, and the
  flat-menu configuration.
- Increment the Core library version code from 147 to 148.

## 2.2.0-MOTHER-MENTOR-ALPHA-10-MOH-SNAPSHOT — 2026-10-24

### Changed

- Upgrade Mother Mentor to `0.1.9-SNAPSHOT` to prevent saved or cancelled
  enrollment forms reopening after language changes or activity restoration,
  while preserving enrollment details and allowing new requests.
- Increment the Core library version code from 146 to 147.

## 2.2.0-MOTHER-MENTOR-ALPHA-9-MOH-SNAPSHOT — 2026-10-24

### Changed

- Upgrade Mother Mentor to `0.1.8-SNAPSHOT` for previous-visit psychosocial and
  income-generating group defaults and read-only Repeat attendance.
- Increment the Core library version code from 145 to 146.

## 2.2.0-MOTHER-MENTOR-ALPHA-8-MOH-SNAPSHOT — 2026-10-24

### Changed

- Upgrade FP and ASRH to `2.0.1-SNAPSHOT` for their household count and
  membership query fixes.
- Increment the Core library version code from 144 to 145.

### Fixed

- Rebind the shared navigation drawer to the active Activity after register
  transitions and language changes.

- Bound linked-client sync retries and report exhausted attempts as failed without
  recursively overflowing the background service stack when requests fail.
- Apply flavor-specific navigation table mappings before the initial count requests.
- Load single-object and array service configurations without an exception-driven
  format fallback, while preserving named table lookup and caching.

## 2.2.0-MOTHER-MENTOR-ALPHA-7-MOH-SNAPSHOT — 2026-10-23

### Changed

- Upgrade Mother Mentor to `0.1.7-SNAPSHOT`, including the merged register UI,
  navigation icons, English/Swahili action headings, and profile referral fixes.
- Increment the Core publication version to
  `2.2.0-MOTHER-MENTOR-ALPHA-7-MOH-SNAPSHOT` and `VERSION_CODE` from 143 to 144.
- Read the Android library version name and code from the release properties.

## 2.2.0-MOTHER-MENTOR-ALPHA-6-MOH-SNAPSHOT — 2026-10-23

### Changed

- Add opt-in collapsible Other Modules navigation with localized labels,
  accessibility actions, and correct iCCM drawer selection.
- Preserve Mother Mentor workflows and the `0.1.5-SNAPSHOT` library dependency.
- Increment the Core publication version to
  `2.2.0-MOTHER-MENTOR-ALPHA-6-MOH-SNAPSHOT` and `VERSION_CODE` from 142 to 143.

## 2.2.0-MOTHER-MENTOR-ALPHA-5-MOH-SNAPSHOT — 2026-10-20

### Changed

- Upgrade Mother Mentor to `0.1.5-SNAPSHOT` for the Swahili partner terminology
  corrections.
- Increment the Core publication version to
  `2.2.0-MOTHER-MENTOR-ALPHA-5-MOH-SNAPSHOT` and `VERSION_CODE` from 141 to 142.

## 2.2.0-MOTHER-MENTOR-ALPHA-4-MOH-SNAPSHOT — 2026-10-20

### Changed

- Upgrade Mother Mentor to `0.1.4-SNAPSHOT`, including attendance and ART-history
  handling and corrected English and Swahili terminology.
- Increment the Core publication version from
  `2.2.0-MOTHER-MENTOR-ALPHA-3-MOH-SNAPSHOT` to
  `2.2.0-MOTHER-MENTOR-ALPHA-4-MOH-SNAPSHOT` and `VERSION_CODE` from 140 to 141.
