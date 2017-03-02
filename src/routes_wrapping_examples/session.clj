(ns routes-wrapping-examples.session
  (:require
   [clojure.java.io :as io]
   [ring.util.response :as resp]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.params :refer [wrap-params]]
   [compojure.core :refer [POST GET]
    :as cc]
   [compojure.route :as route]

   [buddy.auth :as buddy]
   [buddy.auth.backends.session :as session-backend]
   [buddy.auth.middleware :as buddy-mw]
   [buddy.auth.accessrules :as access]
   [buddy.hashers :as hashers]
   [hiccup.core :as hiccup]
   [hiccup.form :as hf]
   ))


(def CTX "/session")


(defn ctx-redirect
  "Just a redirect wrapper that adds our context to the URI. This does
  not work well behind a proxy under a different URL."
  [uri]
  (resp/redirect (format "%s%s" CTX uri)))

(defn login-page
  "Renders the login page.

  This uses hiccup to create some very simple HTML.

  Note, that we are issuing a kind of an API call by referencing an
  image.  Just imagine this is your AJAX call to fetch some additional
  data that should only be visible to logged-in users."
  [req]
  (hiccup/html
   [:body
    [:h1 "Login"]
    [:div "This image will only show when you log in."
     [:img {:src "api/lambda.png"}]]
    (hf/form-to
     [:post "login"]
     [:input {:type "text" :placeholder "username"
              :name "username"}]
     [:input {:type "password" :placeholder "password"
              :name "password"}]
     [:input {:type "submit" :value "Login"}])]))

(defn logout
  "Forwarding to the login page again and removing the session from
  the response. This makes the session middleware delete the session
  from the in-memory session store."
  [req]
  (-> (ctx-redirect "/login")
      (assoc :session nil)))


(defn home
  "This is the area for logged in users.

  In this function, we explicitly ask buddy if the request is
  authenticated to be able to show the visitor different pages.

  Note the 'API' call to lambda.png again which should show up for
  logged-in people but did not work on the login page."
  [req]
  (if-not (buddy/authenticated? req)
    (ctx-redirect "/login")
    (hiccup/html
     [:body
      [:h1 "Welcome"]
      [:div "You have entered the session."]
      [:img {:src "api/lambda.png"}]
      [:div [:a {:href "logout"} "Logout"]]])))

;; Placeholder for a real user DB.
(def user-db
  {"u1" (hashers/derive "p1")
   "u2" (hashers/derive "p2")})

(defn handle-login
  "Receive login-data as POST."
  [req]
  (let [req-user (get-in req [:form-params "username"])
        req-pass (get-in req [:form-params "password"])
        session (:session req)
        lookup-p (user-db req-user)]
    ;; known user and correct password?
    (if (and lookup-p
             (hashers/check req-pass lookup-p))
      ;; Yes, so we can forward the user to the home page and put the
      ;; identity into the session.  Buddy is looking for this key.
      (-> (ctx-redirect "/")
          (assoc :session
                 (assoc session :identity req-user)))
      (ctx-redirect "/login"))))

(def page-routes
  (cc/routes
   (GET "/" [] home)
   (GET "/login" [] login-page)
   (POST "/login" [] handle-login)
   (GET "/logout" [] logout)))

(def api-routes
  (cc/context
   "/api" []
   ;; Earlier, we had authentication in our methods. Here we wrap it
   ;; around the routes using the restrict middleware. You could write
   ;; your own handler for example to add logging, but we are just
   ;; using the default authenticated? method.
   (access/restrict
    (cc/routes
     (GET "/lambda.png" []
          {:status 200
           :content-type "image/png"
           :body (io/input-stream (io/resource "lambda.png"))}))
    {:handler buddy/authenticated?})))

;; Important: You must use the same wrap-session for both parts of
;; your site (page and api) because otherwise it will use different
;; in-memory stores for the session data in the backend and logging in
;; on the page route will not pass the user identity to the API call. 
(defn session-context []
  (-> (cc/context
       CTX []
       page-routes
       api-routes)
      (buddy-mw/wrap-authentication (session-backend/session-backend))
      wrap-params
      wrap-session))
