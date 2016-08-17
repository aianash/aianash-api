@()(implicit req: RequestHeader)

// event accumulator
//   - access importance
//   - update history
//   - decide when to send updates
// capture event (mouse and scroll)
//   - cmpatible to diff browser

(function(){
  var win  = window,
      doc  = document,
      html = doc.documentElement,
      body = doc.body;

  var i;

  var ttol = 300,
      xtol = 20,
      ytol = 20,
      scrollTop = 0,
      scrollLeft = 0,
      prevsx = 0,
      prevsy = 0,
      prevmx = 0,
      prevmy = 0,
      prevtstmp = 1 * new Date();

  // utility functions
  var err = function(ex) {
              console.debug(ex);
      },
      noop = function() {},
      enough = function(xdiff, ydiff) {
                var ct = 1 * new Date();
                var td = ct - prevtstmp;
                return (td > ttol || Math.abs(xdiff) >= xtol || Math.abs(ydiff) >= ytol);
      },
      listen = function(el, event, fn, useCapture) {
                try {
                  el.addEventListener ? el.addEventListener(event, fn, !!useCapture) : el.attachEvent && el.attachEvent("on" + event, fn);
                } catch (ex) {
                  err(ex);
                }
      },
      crimg = function(src) {
                var img = doc.createElement("img");
                img.width = 1;
                img.height = 1;
                img.src = src;
                return img;
      },
      readyList = [],
      onReady = function(fn) {
        readyList.push(fn);
      },
      ready = function() {
        if(isReady) return;
        isReady = true;
        for(i = 0; i < readyList.length, readyList[i](), i++);
      };

  // transport functions
  var nbeacon = function(url, data, success) {
                return win.navigator.sendBeacon ? win.navigator.sendBeacon(url, data) ? (success(), true) : false : false;
      },
      ajax = function(url, data, success) {
                try {
                  var xhr = new win.XMLHttpRequest();
                } catch(e) {}

                if(xhr && "withCredentials" in xhr) {
                  xhr.open("POST", url, false);
                  xhr.withCredentials = false;
                  xhr.setRequestHeader("Content-Type", "text/plain");
                  xhr.onreadystatechange = function() {
                    4 == xhr.readyState && (success(), xhr = null)
                  };
                } else if(typeof XDomainRequest != "undefined") { // IE 8, 9
                  xhr = new XDomainRequest();
                  xhr.open("POST", url);
                  xhr.onload = function() {
                    success(), xhr = null;
                  }
                } else return false;
                xhr.send(data);
                return true;
      },
      imgsend = function(url, data, success) {
                var img = crimg(url + "?" + data);
                img.onload = img.onerror = function() {
                  img.onload = null;
                  img.onerror = null;
                  success();
                }
      },
      send = function(url, data, success) {
                success = success || noop;
                if(data.length <= 2036) imgsend(url, data, success);
                else if(data.length <= 8192) nbeacon(url, data, success) || ajax(url, data, success) || imgsend(url, data, success);
                else return err(), false;
      };

  var sndurl = function() {
                return (ishttps() ? "https:" : "http") + "//www.aianash.com/notify"
      };

  var acc = function() {};

  // set scroll offset
  acc.scroll = function(x, y) {
    if(enough(x - prevsx, y - prevsy)) {
      prevsx = x;
      prevsy = y;

      console.log("s(" + x + ", " + y + ")");
    }
  }

  // append mouse position relative to
  // window
  acc.append = function(x, y) {
    if(enough(x - prevmx, y - prevmy)) {
      console.log("m(" + x + ", " + y + ")");
      prevmx = x;
      prevmy = y;
    }
  }

  function onScroll(e) {
    scrollLeft = (win.pageXOffset || html.scrollLeft || body.scrollLeft || 0);
    scrollTop = (win.pageYOffset || html.scrollTop || body.scrollTop || 0);
    acc.scroll(scrollLeft, scrollTop);
  }

  function onMouseMove(e) {
    try {
      e = e || win.event;
      acc.append(e.clientX, e.clientY);
    } catch(ex) {
      err(ex);
    }
  }

  onReady(function() {
    if(win.aian) {
      var aian = win.aian;
      var token = aian.q[0] == "token" && aian.q[1];
      var sections = doc.getElementsByClassName('aianash');
      for(i = 0; i < sections.length; listen(sections[i], 'mousemove', onMouseMove, false), i++);
      listen(win, 'scroll', onScroll, false);
    }
  });

  function readycompleted() {
    doc.removeEventListener("DOMContentLoaded", readycompleted, false);
    win.removeEventListener("load", readycompleted, false);
    ready();
  }

  if(doc.readyState === "complete" || (doc.readyState !== "loading" && !html.doScroll)) {
    win.setTimeout(ready);
  } else {
    doc.addEventListener("DOMContentLoaded", readycompleted, false);
    win.addEventListener("load", readycompleted, false); // fallback
  }

  if(win.aian) {
    var aian = win.aian;
    aian.init = function() {
      var token = aian.q[0] == "token" && aian.q[1];
      var sections = doc.getElementsByClassName('aianash');
      for(i = 0; i < sections.length; listen(sections[i], 'mousemove', onMouseMove, false), i++);
      listen(win, 'scroll', onScroll, false);
    }
    aian.init();
  }

  console.log("Hello Universe ! I am AIA Nash.");
})(window);