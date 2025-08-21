This is the RevPiPyMachineController Framework to be used for different setups and configuration

Its intend is to provide basis to build different physical setup for the different sites (Aachen, Stutgart, Rennes) and also different variants

TODO: see if we can have this framework indenpent from revpimodio2 so we can test is on the developer PC or in CI, 
ie. move the revpimodio2 specific part on the controller application  ( ie. only in test) 

# Getting Started

The project is ready to run as is. You will need Python 3 or later.

## Use a Virtual Environment

You should use a virtual environment `conda` or `venv`

### With conda
use conda environment (where my-env is he name of the environment in the environment.yml file)

```sh
# list avalible env
conda env list
# Create a conda environment with the required packages for this project:
conda env create -f environment.yml
# note: it already install pip: requirement.txt : no need to do pip install -i requirements.txt
# Activate your conda environment
conda activate rppmcontroller
# reload env
conda env remove --name rppmcontroller
conda env create -f environment.yml
# check environment after local modification in the environment itself
pip freeze
conda env export --from-history
conda env export 
```

## Install the Development Environment

Now run:

```
pip install -e . --config-settings editable_mode=compat
```

(Note: the `--config-settings editable_mode=compat` allows to fix pylance issue with other projects https://stackoverflow.com/questions/76532312/pylance-does-not-recognize-local-packages-installed-with-pip-install-e)

This will install the packages the project depends on in production as well as packages needed during development.

* The `-e` option specifies that you wish to install the package in "editable" mode for development.
* The `.[dev]` argument directs pip to install the package that is defined by the `setup.py` file the in the current directory and to additionally install the extra dependencies defined in the "dev" group. The additional dependencies include things like the Sphinx documentation generator, pytest, pylint and other development packages that end-users of the package will not need.

Refer to the [pip install documentation](https://pip.pypa.io/en/stable/reference/pip_install/#) for more information on these options.

At this point, you are ready to start modifying to template for your own needs.

## Test

Launch the test using

```
pytest
```

optionaly filter test using the `-k` filter

ex:
```
pytest -k "ConveyorBelt"
```

# Informations about the machines

## Vacuum Gripper Robot (VGR)

Once setup: maximum physical observed values are:
- 0 <= vacuumSensVerticalEncoderCounter <= 1750
- 0 <= vacuumSensArmEncoderCounter <= 1970
- 0 <= vacuumSensRotEncoderCounter <= 3040

## HighBay Warehouse (HBW)

Once setup: maximum physical observed values are:
- 0 <= vacuumSensVerticalEncoderCounter <= 1750
- 0 <= vacuumSensHorizontalEncoderCounter <= 4000
