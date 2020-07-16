# Changelog
All notable changes to `knotx-junit5` will be documented in this file.

## Unreleased
List of changes that are finished but not yet released in any final version.
- [PR-62](https://github.com/Knotx/knotx-junit5/pull/62) - Fixes `RequestUtil` failure verification: assertion error not propagated.
                
## 2.2.1
No notable changes.
                
## 2.2.0
- [PR-56](https://github.com/Knotx/knotx-junit5/pull/56) - Move `HoconLoader` from the `Fragments` module to `JUnit5`.

## 2.1.0
- [PR-39](https://github.com/Knotx/knotx-junit5/pull/39) - Fixed missing content-type header for ClasspathResourcesMockServer files

## 2.0.0
No notable changes.

## 1.5.0
- [PR-29](https://github.com/Knotx/knotx-junit5/pull/29) - Add `KnotxAssertions.assertEqualsIgnoreWhitespace`.
- [PR-28](https://github.com/Knotx/knotx-junit5/pull/28) - Migrate from Gradle 4.X to 5.4.1.
- [PR-27](https://github.com/Knotx/knotx-junit5/pull/27) - Unit tests for KnotxWiremockExtension, rename `KnotxWiremock` to `ClasspathResourcesMockServer`.

## 1.4.0
Initial open source release.
- [PR-14](https://github.com/Knotx/knotx-junit5/pull/14), [PR-15](https://github.com/Knotx/knotx-junit5/pull/15), [PR-20](https://github.com/Knotx/knotx-junit5/pull/20) - Random ports support.
- [PR-11](https://github.com/Knotx/knotx-junit5/pull/11) - Load configuration from more than one file, add override strategy.
- [PR-10](https://github.com/Knotx/knotx-junit5/pull/10) - Introduce Wiremock support in tests.
- [PR-6](https://github.com/Knotx/knotx-junit5/pull/6), [PR-7](https://github.com/Knotx/knotx-junit5/pull/7) - Enable HOCON syntax in test configuration files.
- [PR-5](https://github.com/Knotx/knotx-junit5/pull/5) - Html Markup Assertion to test html body content
