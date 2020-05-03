build: Server/node_modules Server/wwwroot/js/grammar.js Server/wwwroot/js/threadingDemo.js
	# Done!

Server/node_modules: Server/package-lock.json Server/package.json
	cd Server && npm install

Server/wwwroot/js/grammar.js: Grammar/grammar.ne Server/node_modules
	node Server/node_modules/nearley/bin/nearleyc.js Grammar/grammar.ne -o Server/wwwroot/js/grammar.js

Server/wwwroot/js/threadingDemo.js: $(shell find Scala/src/* -type f)
	cd Scala && sbt fastOptJS

run: build
	# Launch server in a new terminal.
	# gnome-terminal --working-directory="$(shell pwd)/Server" -- dotnet run
	cd Server && dotnet run
	# Done! Server running.

clean:
	rm -f Server/wwwroot/js/grammar.js
	rm -f Server/wwwroot/js/threadingDemo.js*
	rm -fr Server/node_modules
