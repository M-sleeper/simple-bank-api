# Simple Bank API

## Quick start

### Starting the database using docker

```
docker run --name simple_bank --network=host -e POSTGRES_PASSWORD=password -e POSTGRES_DB=simple_bank -e POSTGRES_USER=simple_bank -d postgres
```

### Starting the server

```
clj -M:server
```
The swagger is available at http://localhost:8080


## Local development

### Connect to REPL using emacs&cider

```
clj -M:dev:cider-nrepl
```
Then connect to the repl with emacs. To start the system evaluate `(go)` in `dev/user.clj`

## Running tests

```
clj -M:test:test-runner
```
