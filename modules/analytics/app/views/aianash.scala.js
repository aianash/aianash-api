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
      body = doc.body,
      aian;

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
  var noop = function() {},
      err = function(ex) {
        console.debug(ex);
      },
      safefn = function(obj, name, fn) {
        obj[name] = function() {
          try {
            return fn.apply(this, arguments)
          } catch (ex) {
            err();
            throw ex;
          }
        }
      },
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
      isReady = false,
      readyList = [],
      onready = function(fn) {
        readyList.push(fn);
      },
      ready = function() {
        if(isReady) return;
        isReady = true;
        for(i = 0; i < readyList.length; readyList[i](), i++);
      };

  var fn = {};
  fn.papply = function(thiz, args) {
    var f = args[0];
    return (f in thiz) ? thiz[f].apply(thiz, [].slice.call(args, 1)) : undefined;
  };
  fn.mapply = function(thiz, fns) {
    for(i = 0; i < fns.length; fn.papply(thiz, fns[i]), i++);
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
        else {
          err();
          return false;
        }
      };

  var sndurl = function() {
        return (ishttps() ? "https:" : "http") + "//www.aianash.com/notify"
      };

  // events
  var evtclbk = function() {
        var args = arguments;
        return function(event) { aian.apply(aian, args); }
      },
      watch = function(section, id) {
        listen(section, 'mouseover', evtclbk('mousein', id), false);
        listen(section, 'mouseout', evtclbk('mouseout', id), false);
      },
      capscroll = function(event) {
        x = (win.pageXOffset || html.scrollLeft || body.scrollLeft || 0);
        y = (win.pageYOffset || html.scrollTop || body.scrollTop || 0);
        if(enough(x - prevsx, y - prevsy)) {
          prevsx = x;
          prevsy = y;
          aian('scroll', x, y);
        }
      };

  /**
   * [aian description]
   */
  aian = function() { return fn.papply(aian, arguments); };
  aian.token = "";
  aian.send = function() {};
  aian.token = function(token) {
    this.token = token;
  };
  aian.scroll = function(x, y) { console.log(x, y); };
  aian.mousein = function(id) { console.log(id); };
  aian.mouseout = function(id) { console.log(id); };
  aian.track = function() {
    var sections = doc.getElementsByClassName('aianash');
    for(i = 0; i < sections.length; watch(sections[i], i), i++);
    listen(win, 'scroll', capscroll, false);
  };

  function aiansetup() {
    var oaian = win["aian"];
    if(oaian) {
      aian.V = oaian.v; // when page was visited
      var _aian = win["aian"] = aian;
      safefn(_aian, "token", _aian.token);
      safefn(_aian, "track", _aian.track);
      safefn(_aian, "send", _aian.send);
      // call pending functions
      // console.debug(oaian.p);
      oaian.p && fn.mapply(_aian, oaian.p);
      // aian.setup();
    }
  };

  onready(aiansetup);

  /** Run on ready state **/
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

  console.log("Hello Universe ! I am AIA Nash.");
})(window);