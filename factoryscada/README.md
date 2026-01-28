# FactorySCADA

WebUI to send commands to the rppmcontroller.

## Running

Here is how you can run the project for development:

### Backend

```bash
cd backend
gradle :bootRun
```

### Frontend

Run `npm install` once before trying to start the frontend.

```bash
cd frontend
npm install
```

After that, start the frontend normally:

```bash
cd frontend
npm run start 
```

## Adding UI for a new command

After a new command has been added in the rppmcontroller, one might want to add a UI for it.
In order to do so, one needs to perform the following steps:

### Adding a template for a debug command

If the command shall be usable in the debug view, it needs to be added to the `command-placeholder.yml` in the backend
resources.

### Create a direct command

In order for the command to be usable as direct command, some infrastructure needs to be added to be backend:

In the `backend` module, ensure that the machine exists in `io.github.mbdo.factoryscada.domains` and then create a
command class in the respective `commands` sub-package.

In the machine class itself, create a method with the respective command name.
The naming convention should follow the one in the rppmcontroller.

Then head to the `io.github.mbdo.factoryscada.frontend.controller` package, find the respective machine controller and
add a new receive-method for the command.

After that, the command can be selected in the UI, but it can't be executed yet.

In the `frontend` module, head to `src/app/pages/direct-command/direct-command.component.html`.
Find the machine in the switch-case-statement and add another case for your command.
The name of the command you are matching against needs to be the same as the above-mentioned method name in the machine
class in the backend. 



