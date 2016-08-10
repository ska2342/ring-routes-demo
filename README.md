# Ring Routes Demo

Demo for some of the less common usage patterns of Clojure Ring.

Most tutorials on [ring](https://github.com/ring-clojure) and
[compojure](https://github.com/weavejester/compojure) out there just
show you how to set up a few routes and then push your complete app
through some middleware to get going.

This demo outlines some patterns for the following use-cases: 

* Plain basic serving of responses without any magic.
* Serving static files from unusual places and with custom URIs.
* Serving a static `robot.txt`
* Using a [compojure](https://github.com/weavejester/compojure)
  [context](https://github.com/weavejester/compojure/wiki/Nesting-routes).
* Calling out to functions defining the routes outside of the common
  `defroutes` macro.
* Wrapping only some of your routes into some ring middleware.
* Using selective wrapping to add HTTP basic auth with
  [buddy](https://github.com/funcool/buddy). 
* A slightly more contrived example how to add
  [buddy session handling](https://funcool.github.io/buddy-auth/latest/#session)
  to allow logging in and in the background protect some API calls
  with the same session.

## Run the Demo

This project does not have any main methods so that you could run the
code easily.  I expect you to be familiar with your Clojure IDE.  For
me, just opening one of the sources files and run `M-x cider-jack-in`
in my Emacs works well.

Actually, this project does not have to be run at all.  It should
serve as an inspiration for you how to organize your routes and
middleware when your application starts to grow.

## License

Copyright © 2016 Stefan Kamphausen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
