# [1.9.0](https://github.com/amplitude/experiment-android-client/compare/1.8.1...1.9.0) (2023-04-24)


### Features

* add support for groups on user object ([#23](https://github.com/amplitude/experiment-android-client/issues/23)) ([c94b68a](https://github.com/amplitude/experiment-android-client/commit/c94b68a622aedaea48dc7340ab512f6d40d83d39))

## [1.8.1](https://github.com/amplitude/experiment-android-client/compare/1.8.0...1.8.1) (2022-12-23)


### Bug Fixes

* use in memory cache for variant access from storage ([#22](https://github.com/amplitude/experiment-android-client/issues/22)) ([cb02a2c](https://github.com/amplitude/experiment-android-client/commit/cb02a2cd4b42588faf866891496af4188726ef34))

# [1.8.0](https://github.com/amplitude/experiment-android-client/compare/1.7.0...1.8.0) (2022-11-15)


### Features

* support subset flags fetch ([#19](https://github.com/amplitude/experiment-android-client/issues/19)) ([efb987c](https://github.com/amplitude/experiment-android-client/commit/efb987c0dd945ca656374beabb2c8fdc61bc2fbc))

# [1.7.0](https://github.com/amplitude/experiment-android-client/compare/1.6.3...1.7.0) (2022-10-19)


### Features

* flag config manipulate ([#18](https://github.com/amplitude/experiment-android-client/issues/18)) ([557c04e](https://github.com/amplitude/experiment-android-client/commit/557c04e2bedceae2ef71ac66921251a00897f657))

## [1.6.3](https://github.com/amplitude/experiment-android-client/compare/1.6.2...1.6.3) (2022-07-29)


### Bug Fixes

* increase integration timeout from 1 to 10 seconds ([1dbb585](https://github.com/amplitude/experiment-android-client/commit/1dbb585ea7bffdc2c741a10013c4c1d3b52bbc1b))

## [1.6.2](https://github.com/amplitude/experiment-android-client/compare/1.6.1...1.6.2) (2022-07-19)


### Bug Fixes

* fix library in user merge ([40db9cb](https://github.com/amplitude/experiment-android-client/commit/40db9cb566728ee60afd24909ac975cf641ed5d9))

## [1.6.1](https://github.com/amplitude/experiment-android-client/compare/1.6.0...1.6.1) (2022-06-02)


### Bug Fixes

* add secondary initial variants as a fallback ([9e81778](https://github.com/amplitude/experiment-android-client/commit/9e81778578ea4a3ca4f04dc639e6114e9cd0708f))

# [1.6.0](https://github.com/amplitude/experiment-android-client/compare/1.5.1...1.6.0) (2022-04-15)


### Features

* invalidate exposure cache on user identity change ([#16](https://github.com/amplitude/experiment-android-client/issues/16)) ([386936f](https://github.com/amplitude/experiment-android-client/commit/386936f6dccdb6c932fdae3e7bd8c6164d9b53c5))

## [1.5.1](https://github.com/amplitude/experiment-android-client/compare/1.5.0...1.5.1) (2022-02-12)


### Bug Fixes

* exposure tracking provider in config builder ([6b172d5](https://github.com/amplitude/experiment-android-client/commit/6b172d576d6ec24773fce6a6c357543628814cdb))

# [1.5.0](https://github.com/amplitude/experiment-android-client/compare/1.4.0...1.5.0) (2022-02-12)


### Features

* add exposure tracking provider ([#15](https://github.com/amplitude/experiment-android-client/issues/15)) ([5a2a471](https://github.com/amplitude/experiment-android-client/commit/5a2a471ecaf72192d6ec42f32d1467532e1d9412))
* core package for seamless integration with analytics ([#12](https://github.com/amplitude/experiment-android-client/issues/12)) ([95addb3](https://github.com/amplitude/experiment-android-client/commit/95addb37ca123f17ed8462938dcae15e56620371))
* rename amplitude-core to analytics-connector ([#13](https://github.com/amplitude/experiment-android-client/issues/13)) ([da7a11c](https://github.com/amplitude/experiment-android-client/commit/da7a11c9f0cd6619bc1ecf7db6334339d0c54f6c))
* use exposure-v2 in connector analytics provider ([#14](https://github.com/amplitude/experiment-android-client/issues/14)) ([afe9cfc](https://github.com/amplitude/experiment-android-client/commit/afe9cfc9c47fe63550a58be9515117e5548f15d1))

# [1.4.0](https://github.com/amplitude/experiment-android-client/compare/1.3.0...1.4.0) (2021-10-18)


### Features

* unset user properties when variant evaluates to null or is a fa… ([#11](https://github.com/amplitude/experiment-android-client/issues/11)) ([8a38ff8](https://github.com/amplitude/experiment-android-client/commit/8a38ff8bdc96b9e37e2c2a689ccefe0f6ecf642d))

# [1.3.0](https://github.com/amplitude/experiment-android-client/compare/1.2.0...1.3.0) (2021-08-12)


### Features

* send user properties for exposure events ([#10](https://github.com/amplitude/experiment-android-client/issues/10)) ([c427360](https://github.com/amplitude/experiment-android-client/commit/c427360a8c301a2933aebe2d75ea46a7e606689e))

# [1.2.0](https://github.com/amplitude/experiment-android-client/compare/1.1.1...1.2.0) (2021-07-29)


### Bug Fixes

* add library to user prior to fetch ([#8](https://github.com/amplitude/experiment-android-client/issues/8)) ([eeb473a](https://github.com/amplitude/experiment-android-client/commit/eeb473a53d04bb0f8c090232d7ac329afada4112))
* revert from post to get with user in header ([#9](https://github.com/amplitude/experiment-android-client/issues/9)) ([c4155f8](https://github.com/amplitude/experiment-android-client/commit/c4155f8fd9b07d4966353e73073a2f95d6451593))
* use post rather than get ([#6](https://github.com/amplitude/experiment-android-client/issues/6)) ([bb2d2eb](https://github.com/amplitude/experiment-android-client/commit/bb2d2eb5c58659088446dc98ff70c894e1116698))


### Features

* automatic exposure tracking through analytics provider ([#7](https://github.com/amplitude/experiment-android-client/issues/7)) ([6cc44ab](https://github.com/amplitude/experiment-android-client/commit/6cc44ab7d0285a164b24aa240012efb1f3143d5a))

## [1.1.1](https://github.com/amplitude/experiment-android-client/compare/1.1.0...1.1.1) (2021-06-29)


### Bug Fixes

* formatting ([d455e87](https://github.com/amplitude/experiment-android-client/commit/d455e87d91b73a1eea592d14fc2343fe6bd37c63))
* use DefaultUserProvider in client by default ([#4](https://github.com/amplitude/experiment-android-client/issues/4)) ([5742e36](https://github.com/amplitude/experiment-android-client/commit/5742e3686f8c99e37d26d6f86279ca0c580633be))

# [1.1.0](https://github.com/amplitude/experiment-android-client/compare/1.0.0...1.1.0) (2021-06-28)


### Features

* add automatic background retries to failed fetch requests ([#3](https://github.com/amplitude/experiment-android-client/issues/3)) ([20a84e5](https://github.com/amplitude/experiment-android-client/commit/20a84e5ada0400d1aac6ab6c8a48c704256f5c2f))
