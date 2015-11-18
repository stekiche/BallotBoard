package controllers;

import models.Ballot;
import models.User;
import org.bson.types.ObjectId;

import play.mvc.Controller;
import play.mvc.Result;

import play.data.*;
import static play.data.Form.*;

import play.mvc.Security;
import views.html.*;

public class Application extends Controller {

    private String currentUser = "";
    private User userObject = null;
    /**
     * Handles requests to /login route
     * @return login form
     */
    public Result login() {
        return ok(login.render(form(Login.class)));
    }

    /**
     * Logs out current user
     * @return
     */
    public Result logout() {
        session().clear();
        flash("success", "You've been logged out");
        currentUser = "";
        return redirect(
                routes.Application.login()
        );
    }
    /**
     * Authenticates a user
     * @return
     */
    public Result authenticate() {

        Form<Login> loginForm = form(Login.class).bindFromRequest();

        if (loginForm.hasErrors()) {
            return badRequest(login.render(loginForm));
        } else {
            userObject = User.findByEmail(loginForm.get().email);
            session().clear();
            session("email", loginForm.get().email);
            session("userObject", userObject.toString());
            currentUser = loginForm.get().email;
            return redirect(
                    routes.Application.ballot()
            );
        }
    }

    /**
     * Creates a new user
     * @return
     */
    public Result register() {
        Form<SignUp> signUpForm = form(SignUp.class).bindFromRequest();

        if (signUpForm.hasErrors()) {
            return badRequest(signup.render(signUpForm));
        } else {
            User newUser = new User();
            newUser.create(signUpForm.get().username, signUpForm.get().fullname,
                    signUpForm.get().email, signUpForm.get().password);
            newUser.insert();
            userObject = User.findByEmail(signUpForm.get().email);
            session().clear();
            // session("username", signUpForm.get().username);
            session("email", signUpForm.get().email);
            currentUser = signUpForm.get().email;
            return redirect(
                    routes.Application.ballot()
            );
        }
    }

    /**
     * Handles request to the main page /index
     * @return
     */
    public Result index () {
        return ok(index.render("Welcome to BallotBoard"));
    }

    /**
     * Handles requests to /signup for creation of new accounts
     * @return
     */
    public Result signup() {
        return ok(signup.render(form(SignUp.class)));
    }

    /**
     * Create a new ballot
     * @return
     */
    @Security.Authenticated(Secured.class)
    public Result create() {
        Form<BallotForm> bForm = form(BallotForm.class).bindFromRequest();

        if (bForm.hasErrors()) {
            return badRequest(ballotForm.render(bForm));
        } else {
            Ballot tmp = new Ballot();
            tmp.create(bForm.get().ballotName, bForm.get().description, request().username());
            tmp.insert();
            Ballot tmp2 = Ballot.findAll().get((Ballot.findAll()).size()-1);
            return redirect(
                    routes.Application.ballotView(tmp2.id.toString())
            );
        }
    }

    /**
     *
     * @return
     */
    @Security.Authenticated(Secured.class)
    public Result ballotForm() {
        return ok(ballotForm.render(form(BallotForm.class)));
    }

    // /**
    //  * Vote on a ballot
    //  * @return
    //  */
    // @Security.Authenticated(Secured.class)
    // public Result vote(String id) {

    // }

    /**
     * Handles /user route
     * @return
     */
    @Security.Authenticated(Secured.class)
    public Result user() {
        currentUser = session("email");
        userObject = User.findByEmail(currentUser);
        return ok(profile.render(Ballot.findAll(), Ballot.findAll(), request().username(), userObject));
    }

    public Result ballot() {
        currentUser = session("email");
        if (currentUser == null){
            currentUser = "";
        }

        return ok(ballot.render(Ballot.findAll(), currentUser));
    }

    @Security.Authenticated(Secured.class)
    public Result ballotView(String id) {
        currentUser = session("email");
        ObjectId ballotId = new ObjectId(id);
        return ok(ballotView.render(Ballot.findById(ballotId), currentUser, ""));
    }

    @Security.Authenticated(Secured.class)
    public Result ballotVote(String id, Boolean vote) {
        
        userObject = User.findByEmail(session("email"));

        ObjectId ballotId = new ObjectId(id);

        if (!userObject.voted(id)) {
            Ballot.vote(ballotId, vote);
            User.add(userObject, id);
        }
        else {
            currentUser = session("email");
            return ok(ballotView.render(Ballot.findById(ballotId), currentUser, "Sorry. You have already voted."));
        }

        return redirect(
            routes.Application.ballotView(id));
        // in this post we are getting the ballot id and the user and
        // our goal is to increment the vote count of b.id if the user hasn't voted
        // on this ballot id (it's not in the list of votehistory)
        // and then subsequently to add this ballot id to the votehistory list
        // optional: error message
    }

    /**
     * Form that holds login email and password
     */
    public static class Login {

        public String email;
        public String password;
        /**
         * Validates user's input on sign in
         * @return
         */
        public String validate() {
            if (User.authenticate(email, password)) {
                return null;
            }
            return "Invalid Email and/or Password";
        }

    }

    /**
     * Form to store information entered by a new user
     */
    public static class SignUp {

        public String email;
        public String fullname;
        public String username;
        public String password;

        /**
         * Checks if an email or username is already taken
         * @return
         */
        public String validate() {
            if (User.exists(email, username)) {
                return "Email and/or Username already taken";
            }
            return null;
        }
    }

    /**
     * Form for a creating a new Ballot
     */
    public static class BallotForm {

        public String ballotName;
        public String description;

        public String validate() {
            if (Ballot.exists(ballotName, description)) {
                return "It appears you are trying to create a ballot that already exists!";
            }
            return null;
        }
    }

}
