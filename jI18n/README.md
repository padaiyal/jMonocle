<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
<div align="center">
  <h1 align="center">jI18n </h1>
  <p align="center">
    A library to internationalize strings.
    <br />
    <a href="https://github.com/padaiyal/jMonocle/issues/new/choose">Report Bug/Request Feature</a>
  </p>

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![Apache License][license-shield]][license-url] <br>
![Maven build - Ubuntu latest](https://github.com/padaiyal/jMonocle/workflows/Maven%20build%20-%20Ubuntu%20latest/badge.svg?branch=main)
![Maven build - Windows latest](https://github.com/padaiyal/jMonocle/workflows/Maven%20build%20-%20Windows%20latest/badge.svg?branch=main)
![Maven build - MacOS latest](https://github.com/padaiyal/jMonocle/workflows/Maven%20build%20-%20MacOS%20latest/badge.svg?branch=main)
![Publish to GitHub packages](https://github.com/padaiyal/jMonocle/workflows/Publish%20to%20GitHub%20packages/badge.svg)
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
    <li>
        <a href="#license">License</a>
    </li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project
This library helps internationalize strings by retrieving values from specified properties files.

<!-- USAGE -->
## Usage
This project is to be used as a dependency to other projects.
To add this project as a dependency follow these steps:
 1. Download the latest jar for this project from [GitHub packages](https://github.com/orgs/padaiyal/packages?repo_name=jI18n) and place it within 
    the dependant project.
 2. Add the following dependency tag to the pom.xml of the dependant project:
    ```
    <dependency>
        <groupId>org.java.padaiyal.utilities</groupId>
        <artifactId>i18n</artifactId>
        <version>2021.01.13</version>
        <scope>system</scope>
        <systemPath>${basedir}/<PATH_TO_JAR></systemPath>
    </dependency>
    ```
    NOTE: Refer the [GitHub packages](https://github.com/orgs/padaiyal/packages?repo_name=jI18n) 
    / [releases](https://github.com/padaiyal/jMonocle/releases) section for this repo to know 
    the latest released version of this project.


<!-- ROADMAP -->
## Roadmap
See the [open issues](https://github.com/padaiyal/jMonocle/issues) for a list of proposed features (and known issues).

<!-- CONTRIBUTING -->
## Contributing
Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project.
2. Create your branch. (`git checkout -b contribution/AmazingContribution`)
3. Commit your changes. (`git commit -m 'Add some AmazingContribution'`)
4. Push to the branch. (`git push origin contribution/AmazingContribution`)
5. Open a Pull Request.


<!-- LICENSE -->
## License
Distributed under the Apache License. See [`LICENSE`](https://github.com/padaiyal/jMonocle/blob/main/LICENSE) for more information.


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/padaiyal/jI18n.svg?style=for-the-badge
[contributors-url]: https://github.com/padaiyal/jMonocle/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/padaiyal/jI18n.svg?style=for-the-badge
[forks-url]: https://github.com/padaiyal/jMonocle/network/members
[stars-shield]: https://img.shields.io/github/stars/padaiyal/jI18n.svg?style=for-the-badge
[stars-url]: https://github.com/padaiyal/jMonocle/stargazers
[issues-shield]: https://img.shields.io/github/issues/padaiyal/jI18n.svg?style=for-the-badge
[issues-url]: https://github.com/padaiyal/jMonocle/issues
[license-shield]: https://img.shields.io/github/license/padaiyal/jI18n.svg?style=for-the-badge
[license-url]: https://github.com/padaiyal/jMonocle/blob/master/LICENSE
