(ns routes-wrapping-examples.core
  (:require
   [routes-wrapping-examples.session :as example-sess]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.defaults :as defaults]
   [ring.middleware.json :refer [wrap-json-body
                                 wrap-json-response]]
   [ring.util.response :as resp]
   [compojure.core :refer [defroutes routes POST GET]
    :as cc]
   [compojure.route :as route]

   [buddy.auth :as buddy]
   [buddy.auth.backends.httpbasic :as httpbasic]
   [buddy.auth.middleware :as buddy-mw]
   [buddy.hashers :as hashers]
   ))

(defn fn-routes [number]
  (routes
   (GET "/" [] (resp/response (format "FN #%s" number)))))


(defn buddy-auth [req {:keys [username password]}]
  (let [user-db
        {"loki" (hashers/derive "havoc")}]
    (println "Received basic auth username" username)
    (when-let [pw (get user-db username)]
      (when (hashers/check password pw)
        username))))

(defroutes app
  ;; This is just a static route for http://localhost:9090/
  (GET "/" [] (resp/response "Root"))

  ;; This finds files locally in the "files" directory. Usually the
  ;; built makes files under resources/public in the root of the
  ;; project available. This example show how to override the local
  ;; directory default "public" with "files" and allows to access them
  ;; via http://localhost:9090/fstatic/static.txt.  CSS files are good
  ;; examples for this kind of routing.
  (route/resources "/fstatic/"
                   {:root "files"})

  ;; This is an example for the default static file serving. We use it
  ;; for delivering a robots.txt as http://localhost:9090/robots.txt
  (route/resources "/")
  
  ;; Here, we define a context. All GET declarations inside the
  ;; context are relative to /listen:
  ;; http://localhost:9090/listen/
  ;; http://localhost:9090/listen/path
  ;; Note, that the context path ("/listen") must not end in a
  ;; trailing slash.  Otherwise you'll get a HTTP 404 when trying to
  ;; access /listen/path
  (cc/context
   "/listen" []
   (GET "/path" [] (resp/response "Listen path"))
   (GET "/" [] (resp/response "Listen Root")))

  ;; Delegate route setup to a function.
  ;; Must pass lexically scoped num as a parameter
  ;; http://localhost:9090/fn/23/
  (cc/context
   "/fn/:num" [num]
   (fn-routes num))

  ;; Wrapping middleware.
  ;; Most examples on the net show you how to create your routes and
  ;; then wrap everything into some middlewares.  But what to do, when
  ;; you want to wrap different parts of your app into different
  ;; middlewares?  Here is an example with an API that wraps automatic
  ;; JSON body generation and a site that uses HTTP basic auth with
  ;; buddy.
  ;; Just as a reminder: all the routes above are still working.
  ;;
  ;; Access from your browser:
  ;; http://localhost:9090/wrapped/basicauth
  ;; with user and password as defined in the buddy-auth authfn
  ;; above.
  (cc/context
   "/wrapped" []
   ;; Wrapping just the API into JSON middleware. Could also wrap into
   ;; wrap-json-body, but we demo no POST requests here.
   (wrap-json-response
    (routes
     (GET "/api" [] (resp/response {:api true}))))
   ;; Imagine this to be a secured part of your site
   (cc/context
    "/basicauth" req
    (-> (routes
         (GET "/" req
              (if-not (buddy/authenticated? req)
                (buddy/throw-unauthorized)
                (resp/response "Welcome to the secure area"))))
        (buddy-mw/wrap-authentication
         (httpbasic/http-basic-backend
          {:realm "RingWrappedBuddySite" :authfn #'buddy-auth})))
    ))

  ;; Here is how to handle sessions. It uses an in-memory user DB with
  ;; hashed passwords, the default ring in-memory session store and
  ;; protects your API calls with the session that is started when you
  ;; log in. And because that example has much more code, we put it
  ;; into its own namespace.
  ;; Go to http://localhost:9090/session/
  (example-sess/session-context)
  )  

;; Serve the complete app.
;; run (jetty app) in your REPL
(defn jetty [handler]
  (jetty/run-jetty handler {:port 9090 :join? false}))

