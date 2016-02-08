(function() {

  refresh();
  setInterval(refresh, 2000);
  
  function refresh() {
    var http = new XMLHttpRequest();

    http.open("GET", "/stats/general");
    http.addEventListener("load", loadHandler);
    http.send()
  }

  function loadHandler(evt) {
    var data = JSON.parse(evt.target.response);
    $("govURLs").innerHTML = data.governmentHosts;
    $("countries").innerHTML = data.countryCodes;
  }

  function $(id) { return document.getElementById(id); }
}());
