(function () {

  var topkIn = makeChart("#topkIn"),
      topkOut = makeChart("#topkOut");

  refresh();
  setInterval(refresh, 5000);

  function refresh() {
    var http = new XMLHttpRequest();

    http.open("GET", "/stats/topk?k=10");
    http.addEventListener("load", loadHandler);
    http.send();
  }

  function loadHandler(evt) {
    var data = transformResponse(JSON.parse(evt.target.response));

    topkIn.load({ columns: data.inbound });
    topkOut.load({ columns: data.outbound });
  }

  function makeChart(id) {
    return c3.generate({
      bindto: id,
      data: {
        x: 'x',
        columns: [],
        type: "bar"
      },
      axis: {
        x: {
          type: "category",
          categories: []
        }
      },
      legend: { show: false }
    });
  }

  function transformResponse(data) {
    var hostsIn = ['x'],
        countsIn = ['total'];

    var tmp = sortByKeys(data.inbound);
    for(var i=0; i<tmp.length; i++) {
      hostsIn.push(tmp[i]);
      countsIn.push(data.inbound[tmp[i]]);
    }

    var hostsOut = ['x'],
        countsOut = ['total'];

    var tmp = sortByKeys(data.outbound);
    for(var i=0; i<tmp.length; i++) {
      hostsOut.push(tmp[i]);
      countsOut.push(data.outbound[tmp[i]]);
    }

    return {
      inbound: [hostsIn, countsIn],
      outbound: [hostsOut, countsOut]
    };
  }

  function sortByKeys(obj) {
    return Object.keys(obj).sort(function(x,y) {
      return obj[y] - obj[x];
    });
  }

}());
