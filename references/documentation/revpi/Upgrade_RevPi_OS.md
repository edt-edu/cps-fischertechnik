# Upgrade RevPi OS:
This file describes the steps to update the RevPi OS to RevPI Bookworm. 

## Step 1: Save Pictory Config
Use SCP to copy the file **_config.rsc** from the RevPi.

    scp pi@<IP_adress_of_the_RevPi>:/var/www/revpi/pictory/projects/_config.rsc ./<island_number>_config.rsc



## Step 2: Installing new OS
Follow the instructions (Step 2 and Step 5 can be skipped) https://revolutionpi.com/en/knowledge/reinstalling-image  

After the installation the connection via SSH will show a warning:

    @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    @    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @
    @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!
    ...

To delete the old SSH key just follow the instructions provided by the warning.

## Step 3: Set up passwordless SSH login
    ssh-copy-id pi@<IP_adress_of_the_RevPi>

## Step 4: Restore Pictory Config

Copy the **_config.rsc** file to the RevPi using SCP. Run this instruction from the directoy where you saved the _config.rsc file in Step 2.

    scp ./<island_number>_config.rsc pi@<IP_adress_of_the_RevPi>:/var/www/revpi/pictory/projects/_config.rsc

## Step 5: Deactivate the firewall on the RevPi
The firewall was blocking access to the RevPi. As the RevPi is only connected to a local network, simply deactivating the firewall is sufficient.  
Access the RevPi **Cockpit** via the RevPi's IP adress using a web browser. In the menu under **Network** the firewall can be deactivated.

## Step 6: Install dependencies
In **mbdo-impl/RWTH_Setup/scripts** there is a script that has to be run from the control PC connected to the RevPI: 

    ./start_controller.sh <island_number> -d
