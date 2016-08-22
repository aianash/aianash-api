@()(implicit req: RequestHeader)

(function(){
  var win  = window,
      doc  = document,
      html = doc.documentElement,
      body = doc.body,
      math = Math,
      aian;

  var i,
      windowHeight,
      windowWidth;

  /** On Document Ready **/

  var isReady = false,
      readyList = [];

  function onready(fn) {
    readyList.push(fn);
  };

  function ready() {
    if(isReady) return;
    isReady = true;
    for(i = 0; i < readyList.length; readyList[i](), i++);
  };

  function readycompleted() {
    doc.removeEventListener("DOMContentLoaded", readycompleted, false);
    win.removeEventListener("load", readycompleted, false);
    ready();
  }

  /** util **/

  function util_noop() {};

  function util_err(ex) {
    console.error(ex);
  };

  function util_pereq(a, b, per) {
    math.abs(a - b) <= per * (math.max(a, b));
  }

  function util_ishttps() {
    return doc.location.protocol === "https:";
  };

  function util_listen(el, event, fn, useCapture) {
    try {
      if(el.addEventListener) el.addEventListener(event, fn, !!useCapture);
      else if(el.attachEvent) el.attachEvent("on" + event, fn);
    } catch (ex) {
      util_err(ex);
    }
  };

  function util_relxy(ele) {
    var br = ele.getBoundingClientRect();
    return [math.round(br.left), math.round(br.top)];
  }

  function util_mk1pxImg(src) {
    var img = doc.createElement("img");
    img.width = 1;
    img.height = 1;
    img.src = src;
    return img;
  };

  function util_isoutside(parent, event) {
    var ele = event.relatedTarget || event.toElement || event.fromElement;
    while(ele && ele !== parent) {
      ele = ele.parentNode;
    }
    if(ele !== parent) return true;
  };

  function util_deltacode(obj, k, prev) {
    var tmp = obj[k];
    obj[k] = tmp - prev;
    return tmp;
  };

  // function util_lzw(s) {

  // }

  /** fn **/

  function fn_setsafe(obj, name, fn) {
    obj[name] = function() {
      try {
        return fn.apply(this, arguments)
      } catch (ex) {
        util_err();
        throw ex;
      }
    }
  }

  function fn_papply(thiz, args) {
    var f = args[0];
    return (f in thiz) ? thiz[f].apply(thiz, [].slice.call(args, 1)) : undefined;
  }

  function fn_mapply(thiz, fns) {
    for(i = 0; i < fns.length; fn_papply(thiz, fns[i]), i++);
  };

  /** transport **/

  function transport_beacon(url, data, success) {
    return win.navigator.sendBeacon ? win.navigator.sendBeacon(url, data) ? (success(), true) : false : false;
  };

  function transport_ajax(url, data, success) {
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
  };

  function transport_imgsend(url, data, success) {
    var img = util_mk1pxImg(url + "?" + data);
    img.onload = img.onerror = function() {
      img.onload = null;
      img.onerror = null;
      success();
    }
  };

  function transport_send(url, data, success) {
    success = success || util_noop;
    if(data.length <= 2036) transport_imgsend(url, data, success);
    else if(data.length <= 8192)
      transport_beacon(url, data, success) || transport_ajax(url, data, success) || transport_imgsend(url, data, success);
    else {
      util_err();
      return false;
    }
  };

  /** accumulator **/

  var acc_analyticsUrl = (util_ishttps() ? "https:" : "http:") + "//www.aianash.com/analytics/notify",
      acc_events = [],
      acc_mouseInTm = {},
      acc_mouseInXY = {};

  function util_lzw(state, data) {
    var dict = state['d'];
    var phrase = state['p'];
    var res = state['r'];
    var code = state['c'];
    var currChar;
    for(var i = 0; i < data.length; i++) {
      currChar = data[i];
      if(dict[phrase + currChar] != null) {
        phrase += currChar;
      } else {
        res += String.fromCharCode(phrase.length > 1 ? dict[phrase] : phrase.charCodeAt(0));
        dict[phrase + currChar] = code;
        code++;
        phrase = currChar;
      }
    }
    state['p'] = phrase;
    state['r'] = res;
    state['c'] = code;
  }

  function acc_encode(events) {
    var prevTm = aian.V;
    var prevDur = 0;
    var datastr = prevTm + "";
    var data = datastr.split("");

    var state = {d: {}, p: data[0], c: 256, r: ""};
    util_lzw(state, [].slice.call(data, 1));

    for(i = 0; i < events.length; i++) {
      var event = events[i];
      var evtype = event[0];

      prevTm = util_deltacode(event, 1, prevTm);

      if(evtype === 'r') {
        event[5] = event[5] - event[3];
        event[6] = event[6] - event[4];
      } else if(evtype === 'p' && event.length > 4) {
        var pathele = event[3];
        var prevx = pathele[1];
        var prevy = pathele[2];
        for(var j = 4; j < event.length; j++) {
          pathele = event[j];
          prevx = util_deltacode(pathele, 1, prevx);
          prevy = util_deltacode(pathele, 2, prevy);
        }
      }

      util_lzw(state, [].join.call(event, ',').split(""));
    }

    var res = state['r'];
    var phrase = state['p'];
    res += String.fromCharCode(phrase.length > 1 ? state['d'][phrase] : phrase.charCodeAt(0));
    return res;
  };

  function acc_append() {
    var args = arguments;
    acc_events.push(args);
    var encoded;
    if(acc_events.length > 20) {
      encoded = acc_encode(acc_events);
      acc_events = [];
      transport_send(acc_analyticsUrl, encoded, util_noop);
    }
  };

  /** path event constructors **/

  var path = [],
      path_latestTimestamp = 0,
      path_active = false;

  function path_useExisting() {
    return path_active && ((1 * new Date()) - path_latestTimestamp) < 2000;
  };

  function path_new(startTm) {
    path_end(); // end any existing path
    path_active = true;
    path = ['p', startTm, 0];
  };

  function path_end() {
    path_active && (path_active = false, acc_append.apply(null, path));
  };

  function path_append(id, xy, tm) {
    path_latestTimestamp = tm;
    path[2] = tm - path[1];
    path.push([id, xy[0], xy[1]]);
  };

  /** read event constructors */

  var read_curr = [],
      read_active = false;

  function read_start(tm, dur, x, y) {
    read_active = true;
    read_curr = ['r', tm, dur, x, y, 0, 0];
  }

  function read_end() {
    read_active && (read_active = false, acc_append.apply(null, read_curr));
  }

  function read_update(tm, dur, x, y) {
    if(read_active) {
      read_curr[2] = read_curr[2] + dur;
      read_curr[5] = x;
      read_curr[6] = y;
      return true;
    } else {
      read_end()
      return false
    };
  }

  /** Event Handlers **/

  var evh_mouseActivity = false,
      evh_prevSkrlX = 0,
      evh_prevSkrlY = 0,
      evh_prevSkrlTm;

  function evh_scroll(event) {
    path_end();

    var currTm = 1 * new Date();
    var prevDur = currTm - evh_prevSkrlTm;

    x = (win.pageXOffset || html.scrollLeft || body.scrollLeft || 0);
    y = (win.pageYOffset || html.scrollTop || body.scrollTop || 0);

    if(prevDur > 700 || evh_mouseActivity) {
      read_end();
      acc_append('f',  evh_prevSkrlTm, prevDur, evh_prevSkrlX, evh_prevSkrlY);
      evh_mouseActivity = false;
    } else read_update(evh_prevSkrlTm, prevDur, evh_prevSkrlX, evh_prevSkrlY) || read_start(evh_prevSkrlTm, prevDur, evh_prevSkrlX, evh_prevSkrlY);

    evh_prevSkrlTm = currTm;
    evh_prevSkrlX = x;
    evh_prevSkrlY = y;
  };

  function evh_mousein(tm, id, event) {
    evh_mouseActivity = true;
    acc_mouseInTm[id] = tm;
    acc_mouseInXY[id] = util_relxy(event.target);
  };

  function evh_mouseout(tm, id, event) {
    evh_mouseActivity = true;
    var startTm = acc_mouseInTm[id];
    if(!startTm) return;
    acc_mouseInTm[id] = undefined;

    var xy = acc_mouseInXY[id],
        dur = (tm - startTm);

    if(dur >= 700) {
      path_end();
      acc_append('s', startTm, dur, id, xy[0], xy[1]);
    } else if(path_useExisting()) {
      path_append(id, xy, tm);
    } else {
      path_new(startTm);
      path_append(id, xy, tm);
    }
  };

  function evh_crMouseHandler(id, fn) {
    return function(event) {
      event = event || win.event;
      if(util_isoutside(this, event)) {
        fn.call(null, 1 * new Date(), id, event);
      }
    }
  };

  function evh_watchMouseEvt(section, id) {
    util_listen(section, 'mouseover', evh_crMouseHandler(id, evh_mousein), false);
    util_listen(section, 'mouseout', evh_crMouseHandler(id, evh_mouseout), false);
  };

  /** AIAN API **/

  aian = function() { return fn_papply(aian, arguments); };
  aian.token = "";

  aian.token = function(token) {
    this.token = token;
  };

  aian.track = function() {
    var sections = doc.getElementsByClassName('aianash');
    for(i = 0; i < sections.length; evh_watchMouseEvt(sections[i], i), i++);
    evh_prevSkrlTm = aian.V;
    util_listen(win, 'scroll', evh_scroll, false);
  };

  onready(function() {
    windowHeight = win.screen.height;
    windowWidth = win.screen.width;
    var oaian = win["aian"];
    if(oaian) {
      aian.V = oaian.v; // when page was visited
      var _aian = win["aian"] = aian;
      fn_setsafe(_aian, "token", _aian.token);
      fn_setsafe(_aian, "track", _aian.track);
      oaian.p && fn_mapply(_aian, oaian.p);
    }
  });

  /** Start Functions registered to onready */
  if(doc.readyState === "complete" || (doc.readyState !== "loading" && !html.doScroll)) {
    win.setTimeout(ready);
  } else {
    doc.addEventListener("DOMContentLoaded", readycompleted, false);
    win.addEventListener("load", readycompleted, false); // fallback
  }

})(window);