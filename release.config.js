module.exports = {
  "branches": ["main"],
  "tagFormat": ["${version}"],
  "plugins": [
    ["@semantic-release/commit-analyzer", {
      "preset": "angular",
      "parserOpts": {
        "noteKeywords": ["BREAKING CHANGE", "BREAKING CHANGES", "BREAKING"]
      }
    }],
    ["@semantic-release/release-notes-generator", {
      "preset": "angular",
    }],
    ["@semantic-release/changelog", {
      "changelogFile": "CHANGELOG.md"
    }],
    "@semantic-release/github",
    [
      "@google/semantic-release-replace-plugin",
      {
        "replacements": [
          {
            "files": ["sdk/build.gradle"],
            "from": "PUBLISH_VERSION = \'.*\'",
            "to": "PUBLISH_VERSION = \'${nextRelease.version}\'",
            "results": [
              {
                "file": "sdk/build.gradle",
                "hasChanged": true,
                "numMatches": 1,
                "numReplacements": 1
              }
            ],
            "countMatches": true
          },
        ]
      }
    ],
    ["@semantic-release/git", {
      "assets": ["AmplitudeExperiment.podspec", "sdk/build.gradl", "CHANGELOG.md", "docs/*"],
      "message": "chore(release): ${nextRelease.version} [skip ci]\n\n${nextRelease.notes}"
    }],
    ["@semantic-release/exec", {
      "publishCmd": "./gradlew sdk:publishReleasePublicationToSonatypeRepository",
    }],
  ],
}
