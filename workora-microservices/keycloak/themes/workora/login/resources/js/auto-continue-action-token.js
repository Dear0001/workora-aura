(function () {
  var path = window.location.pathname;
  var isActionToken = path.includes("/login-actions/action-token");
  var isRequiredAction = path.includes("/login-actions/required-action");

  if (!isActionToken && !isRequiredAction) {
    return;
  }

  var showVerificationSuccess = function () {
    if (!document.body || document.querySelector(".workora-verification-card")) {
      return;
    }

    document.body.classList.add("workora-action-token-page");
    var card = document.createElement("main");
    card.className = "workora-verification-card";
    card.setAttribute("role", "status");
    card.setAttribute("aria-live", "polite");
    card.innerHTML =
      '<section class="workora-verification-card__panel">' +
      '<div class="workora-verification-card__logo">◉ WORKORA</div>' +
      '<div class="workora-verification-card__icon" aria-hidden="true">&#10003;</div>' +
      "<h1>Email verified</h1>" +
      "<p>Your Workora account is ready. Redirecting you to sign in...</p>" +
      '<div class="workora-verification-card__progress" aria-hidden="true"></div>' +
      "</section>";
    document.body.appendChild(card);
  };

  var continueAction = function () {
    var links = document.querySelectorAll("a");
    for (var index = 0; index < links.length; index += 1) {
      var text = (links[index].textContent || "").trim().toLowerCase();
      var shouldContinue = isActionToken
        ? text.indexOf("proceed") !== -1 || text.indexOf("continue") !== -1
        : text.indexOf("back to application") !== -1 || text === "application";

      if (shouldContinue) {
        if (isActionToken) {
          var continuationUrl = links[index].href;
          showVerificationSuccess();
          window.setTimeout(function () {
            window.location.assign(continuationUrl);
          }, 3000);
        } else {
          links[index].click();
        }
        return;
      }
    }
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", continueAction);
  } else {
    continueAction();
  }
})();
