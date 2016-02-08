(function() {

  var chart = c3.generate({
    bindto: "#countsByHost",
    data: { x: "x", columns: [] },
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

  refresh();
  setInterval(refresh, 2000);

  function refresh() {
    var http = new XMLHttpRequest();

    http.open("GET", "/stats/counts");
    http.addEventListener("load", loadHandler);
    http.send();
  }

  function loadHandler(evt) {
    var data = transformResponse(JSON.parse(evt.target.response));
    chart.load({ columns: data });
  }

  // TODO: make clubd return this properly formatted...
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

}());
