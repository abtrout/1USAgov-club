(function () {

  var topkIn = makeChart("#topkIn"),
      topkOut = makeChart("#topkOut");

  refresh();
  setInterval(refresh, 5000);

  function makeChart(id) {
    return c3.generate({
      bindto: id,
      data: {
        rows: [],
        type: "bar"
      },
      legend: { show: false }
    });
  }

  function barOrder(a, b) {
    return a.values[0].value > b.values[0].value;
  }

  function refresh() {
    var http = new XMLHttpRequest();

    http.open("GET", "/stats/topk?k=10");
    http.addEventListener("load", loadHandler);
    http.addEventListener("error", errorHandler);
    http.send();
  }

  function loadHandler(evt) {
    var data = transformResponse(JSON.parse(evt.target.response));

    topkIn.load({
      rows: data.inbound,
      unload: topkIn.rows
    });

    topkOut.load({
      rows: data.outbound
      //unload: topkOut.rows
    });
  }

  // We produce arrays (... of arrays) for each host/count; separately
  // parsing incoming and outgoing hosts and returning both.
  function transformResponse(data) {
    var hostsIn = [],
        countsIn = [];

    for(host in data.inbound) {
      hostsIn.push(host);
      countsIn.push(data.inbound[host]);
    }

    var hostsOut = [],
        countsOut = [];

    for(host in data.outbound) {
      hostsOut.push(host);
      countsOut.push(data.outbound[host]);
    }

    return {
      inbound: [hostsIn, countsIn],
      outbound: [hostsOut, countsOut]
    };
  }

  function errorHandler() {
    $("topk").classList.add("hidden");
  }

  function $(id) { return document.getElementById(id); }
}());

