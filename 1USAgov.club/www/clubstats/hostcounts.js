(function() {

  // Host counts are displayed as a timeseries graph with C3.
  var chart = makeChart(),
      refreshInt = null,
      hostnames = [];

  refreshInt = setInterval(refresh, 2000);
  refresh();

  $("submitHosts").addEventListener("click", filterHosts);
  $("hostnames").addEventListener("keypress", function(evt) {
    if(evt.keyCode == 13) filterHosts();
  });

  function makeChart() {
    return c3.generate({
      bindto: "#countsByHost",
      data: {
        x: "x",
        columns: []
      },
      axis: {
        x: {
          type: "timeseries",
          tick: { format: "%H:%M" }
        }
      },
      tooltip: {
        format: {
          title: function(d) { 
            return new Date(d).toLocaleString();
          }
        }
      },
      legend: { show: false }
    });
  }

  // A comma/space delimited list of hostnames can be provided with
  // our requests to /stats/counts to view stats for specific host(s).
  //
  // When this happens, we need to reset our refresh() interval.
  function filterHosts(evt) {
    hostnames = $("hostnames").value
      .replace(/,/g, " ")
      .replace(/\s{2,}/g, " ")
      .split(" ")

    // Clear existing data.
    clearInterval(refreshInt);
    chart = makeChart();

    // Start loading new data for these hostnames instead.
    refreshInt = setInterval(refresh, 2000);
    refresh();
  }

  function refresh() {
    var http = new XMLHttpRequest(),
        url = "/stats/counts";

    if(hostnames.length) {
      url += "?hostnames=" + hostnames.join(",");
    }

    http.open("GET", url);
    http.addEventListener("load", loadHandler);
    http.send();
  }

  function loadHandler(evt) {
    var data = transformResponse(JSON.parse(evt.target.response));
    chart.load({ columns: data });
  }

  // C3 expects data in slightly/very different format than the 1USAgov
  // API responds with. Unfortunately we have to do some munging :|
  function transformResponse(data) {
    var parsed = {};

    for(var i=0; i<data.length; i++) {
      var d = data[i];
      parsed[d.hostname] = parsed[d.hostname] || {};
      parsed[d.hostname][d.ts] = d.total; 
    }

    var dates = ["x"];
    for(host in parsed) {
      for(ts in parsed[host]) {
        if(dates.indexOf(ts) < 0)
          dates.push(parseInt(ts));
      }
    }
    
    var entries = [];
    for(host in parsed) {
      var entry = [host];

      for(i=1; i<dates.length; i++) {
        var date = dates[i];
        entry.push(parsed[host][date] || 0);
      }

      entries.push(entry);
    }

    return [dates].concat(entries);
  }

  // jQuery light!
  function $(id) { return document.getElementById(id); }

}());
