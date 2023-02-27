<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
<div align="center">
  <h1 align="center">jProperties</h1>
  <p align="center">
    A library to load values from pre-specified property files.
    <br />
    <a href="https://github.com/padaiyal/jMonocle/issues/new/choose">Report Bug/Request Feature</a>
  </p>

[![Contributors][contributors-shield]][contributors-url]
[![Issues][issues-shield]][issues-url]
[![Apache License][license-shield]][license-url] <br>
[![Maven build - Ubuntu latest][build-shield]][build-url]
[![Publish package to the Maven Central Repository][publish-workflow-badge]][publish-workflow-url] <br>
[![Latest version in Maven Central][maven-shield]][maven-url]
</div>

<!-- TABLE OF CONTENTS -->
<details open="open">
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
    </li>
    <li>
        <a href="#usage">Usage</a>
    </li>
    <li>
        <a href="#roadmap">Roadmap</a>
    </li>
    <li>
        <a href="#contributing">Contributing</a>
    </li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project
This library helps aggregate a set of property files and retrieve values corresponding
to specific keys in the desired type.

<!-- USAGE -->
## Usage
Add the following dependency tag to the pom.xml of the dependant project:
```
<dependency>
  <groupId>io.github.padaiyal.libs</groupId>
  <artifactId>properties</artifactId>
  <version>2023.12.11</version>
</dependency>
```
NOTE: Refer to the [Maven Repository][maven-url]
/ [releases][releases-url] to know
the latest released version of this project.

Here's a sample snippet showing the usage of PropertyUtility:
```
PropertyUtility.addPropertyFile(I18nUtilityTest.class, "test.properties");
String propertyValue = PropertyUtility.getProperty("generic.property");
```

For more such of examples, checkout [PropertyUtilityTest][lib-test-url]
<!-- ROADMAP -->
## Roadmap
See the [open issues][issues-url] for a list of proposed features (and known issues).

<!-- CONTRIBUTING -->
## Contributing
Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project.
2. Create your branch. (`git checkout -b contribution/AmazingContribution`)
3. Commit your changes. (`git commit -m 'Add some AmazingContribution'`)
4. Push to the branch. (`git push origin contribution/AmazingContribution`)
5. Open a Pull Request.

<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/padaiyal/jProperties.svg?style=for-the-badge
[contributors-url]: https://github.com/padaiyal/jMonocle/graphs/contributors
[issues-shield]: https://img.shields.io/github/issues/padaiyal/jProperties.svg?style=for-the-badge
[issues-url]: https://github.com/padaiyal/jMonocle/issues?q=is%3Aissue+is%3Aopen+label%3Aproperties
[license-shield]: https://img.shields.io/github/license/padaiyal/jProperties.svg?style=for-the-badge
[license-url]: https://github.com/padaiyal/jMonocle/blob/main/libs/properties/LICENSE
[build-shield]: https://github.com/padaiyal/jMonocle/workflows/Maven%20build%20-%20clean%20test%20verify/badge.svg?branch=main
[build-url]: https://github.com/padaiyal/jMonocle/actions/workflows/maven_build.yml?query=branch%3Amain
[publish-workflow-badge]:https://github.com/padaiyal/jMonocle/actions/workflows/maven_central_package_publish.yml/badge.svg
[publish-workflow-url]:https://github.com/padaiyal/jMonocle/actions/workflows/maven_central_package_publish.yml
[maven-shield]: https://img.shields.io/maven-central/v/io.github.padaiyal.libs/properties
[maven-url]: https://central.sonatype.com/artifact/io.github.padaiyal.libs/properties/2023.02.07/versions
[releases-url]: https://github.com/padaiyal/jMonocle/releases
[lib-test-url]: https://github.com/padaiyal/jMonocle/blob/main/libs/properties/src/test/java/tests/org/padaiyal/utilities/PropertyUtilityTest.java
