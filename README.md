# CPS Fischertechnik

This repository contains the software and hardware assets used to operate a **cyber-physical production system (CPPS)** built around Fischertechnik factory modules.

It brings together physical factory components, machine controllers, supervision software, and model-based mission orchestration to provide an experimental smart manufacturing environment. The platform is intended for research, experimentation, demonstration, and teaching around **cyber-physical systems, digital twins, model-driven engineering, and modular factory automation**.

The repository includes a web-based SCADA application for supervising and operating the factory, Python-based controllers implementing machine behavior, tooling for generating production missions from SysML models, as well as supporting CAD models and hardware assets.

This repository focuses on the **physical system and its control software**. The broader experimental environment is organized across complementary repositories:

- [dt-platform](https://github.com/edt-edu/dt-platform) provides the reusable software components used to build and operate digital twins and their supporting infrastructure.
- [dt-setups](https://github.com/edt-edu/dt-setups) contains concrete experimental setups that assemble and configure components from this repository and dt-platform into reproducible digital-twin experiments.

Together, these repositories provide the building blocks for experimenting with different digital-twin architectures, configurations, and cyber-physical scenarios.

## Project structure

* [`factoryscada`](factoryscada) — Web-based SCADA frontend and backend for supervising and interacting with the factory.
* [`rppmcontroller`](rppmcontroller) — Python controller framework implementing machine logic and production behavior.
* [`sysml-mission-generator`](sysml-mission-generator) — Tooling for generating executable factory missions from SysML models.
* [`CAD_models`](CAD_models) — Mechanical designs and 3D CAD models for some factory components. Additional Fischertechnik 3D models are available to the EDT-edu project but cannot be distributed in this public repository due to licensing restrictions.
* [`hardware`](hardware) — Hardware-related extensions, designs, and support files.
* [`references`](references) — Documentation, reference material, and supporting information.

## License

This project is licensed under the Apache License, Version 2.0.

See the [LICENSE](LICENSE) file for details.
