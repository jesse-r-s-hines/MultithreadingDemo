RID ?= linux-x64

build: Server/node_modules grammar threadingDemo
	# Done!

Server/node_modules: Server/package-lock.json Server/package.json
	cd Server && npm ci

grammar: Grammar/grammar.ne Server/node_modules
	mkdir -p Server/wwwroot/js/generated
	node Server/node_modules/nearley/bin/nearleyc.js Grammar/grammar.ne -o Server/wwwroot/js/generated/grammar.js

threadingDemo: $(shell find Scala/src/* -type f)
	mkdir -p Server/wwwroot/js/generated
	cd Scala && sbt fastOptJS

run: build
	# Launch server in a new terminal.
	# gnome-terminal --working-directory="$(shell pwd)/Server" -- dotnet run
	cd Server && dotnet run
	# Done! Server running.

watch: build
	cd Server && dotnet watch run
	# Done! Server running.

publish: build
	cd Server && \
	dotnet publish -c Release -r "$(RID)" --self-contained true -o bin/Release/publish
	# Output in bin/Release/publish

clean:
	rm -rf Server/wwwroot/js/generated
	rm -rf Server/node_modules
	cd Server && dotnet clean && rm -rf bin obj Multithreading.db*
