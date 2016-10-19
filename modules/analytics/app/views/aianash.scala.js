@(analyticsurl: String, pageviewurl: String)(implicit req: RequestHeader)

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
  }

  function ready() {
    if(isReady) return;
    isReady = true;
    for(i = 0; i < readyList.length; readyList[i](), i++);
  }

  function readycompleted() {
    doc.removeEventListener('DOMContentLoaded', readycompleted, false);
    win.removeEventListener('load', readycompleted, false);
    ready();
  }

  /** util **/

  function util_noop() {}

  function util_err(ex) {
    console.error(ex);
  }

  function util_pereq(a, b, per) {
    math.abs(a - b) <= per * (math.max(a, b));
  }

  function util_ishttps() {
    return doc.location.protocol === 'https:';
  }

  function util_listen(el, event, fn, useCapture) {
    try {
      if(el.addEventListener) el.addEventListener(event, fn, !!useCapture);
      else if(el.attachEvent) el.attachEvent('on' + event, fn);
    } catch (ex) {
      util_err(ex);
    }
  }

  function util_relxy(ele) {
    var br = ele.getBoundingClientRect();
    return [math.round(br.left), math.round(br.top)];
  }

  function util_mk1pxImg(src) {
    var img = doc.createElement('img');
    img.width = 1;
    img.height = 1;
    img.src = src;
    return img;
  }

  function util_isoutside(parent, event) {
    var ele = event.relatedTarget || event.toElement || event.fromElement;
    while(ele && ele !== parent) {
      ele = ele.parentNode;
    }
    if(ele !== parent) return true;
  }

  function util_deltacode(obj, k, prev) {
    var tmp = obj[k];
    obj[k] = tmp - prev;
    return tmp;
  }

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
  }

  /** transport **/

  function transport_beacon(url, data, success) {
    return win.navigator.sendBeacon ? win.navigator.sendBeacon(url, data) ? (success(), true) : false : false;
  }

  function transport_ajax(url, data, success) {
    try {
      var xhr = new win.XMLHttpRequest();
    } catch(e) {}

    if(xhr && 'withCredentials' in xhr) {
      xhr.open('POST', url, false);
      xhr.withCredentials = false;
      xhr.setRequestHeader('Content-Type', 'text/plain');
      xhr.onreadystatechange = function() {
        4 == xhr.readyState && (success(), xhr = null)
      };
    } else if(typeof XDomainRequest != 'undefined') { // IE 8, 9
      xhr = new XDomainRequest();
      xhr.open('POST', url);
      xhr.onload = function() {
        success(), xhr = null;
      }
    } else return false;
    xhr.send(data);
    return true;
  }

  function transport_imgsend(url, data, success) {
    var img = util_mk1pxImg(url + '?' + data);
    img.onload = img.onerror = function() {
      img.onload = null;
      img.onerror = null;
      success();
    }
  }

  function transport_send(url, data, success) {
    success = success || util_noop;
    if(data.length <= 2036) transport_imgsend(url, data, success);
    else if(data.length <= 8192)
      transport_beacon(url, data, success) || transport_ajax(url, data, success) || transport_imgsend(url, data, success);
    else {
      util_err();
      return false;
    }
  }

  /** accumulator **/

  var acc_analyticsUrl = (util_ishttps() ? 'https://' : 'http://') + '@analyticsurl',
      acc_events = [],
      acc_mouseInTm = {},
      acc_mouseInXY = {};

  function acc_encode(events) {
    var startTm = prevTm = aian.V;
    var prevDur = 0;
    var data;
    var state;

    for(i = 0; i < events.length; i++) {
      var event = events[i];
      var evtype = event[0];

      prevTm = util_deltacode(event, 1, prevTm);

      data = [event[0]];
      data = data.concat([].slice.call(event, 1).join(',').split(''));

      if(i == 0) {
        state = {d: {}, p: data[0], c: 256, r: ''};
        data = [].slice.call(data, 1)
      }
      util_lzw(state, data);
    }

    var res = state['r'];
    var phrase = state['p'];
    res += String.fromCharCode(phrase.length > 1 ? state['d'][phrase] : phrase.charCodeAt(0));
    res = startTm + res;
    return res;
  }

  function acc_append() {
    var args = arguments;
    acc_events.push(args);
    var encoded;
    if(acc_events.length > 20) {
      encoded = 'd=' + encodeURIComponent(acc_encode(acc_events)) + '&t=' + aian.T;
      acc_events = [];
      transport_send(acc_analyticsUrl, encoded, util_noop);
    }
  }

  /** Event Handlers **/

  // [TODO] to be handled as a seperation ViewChange event
  function evh_resize() {
    windowHeight = html.clientHeight;
    windowWidth = html.clientWidth;
  }

  function evh_scroll(event) {
    var currTm = 1 * new Date();
    var x = math.round(win.pageXOffset || html.scrollLeft || body.scrollLeft || 0);
    var y = math.round(win.pageYOffset || html.scrollTop || body.scrollTop || 0);

    acc_append('s', currTm, x, y, windowWidth, windowHeight);
  }

  function evh_mousemove(event) {
    var currTm = 1 * new Date();
    var x = event.pageX;
    var y = event.pageY;

    acc_append('m', currTm, x, y, windowWidth, windowHeight);
  }

  function evh_click(event) {
    var currTm = 1 * new Date();
    var x = event.pageX;
    var y = event.pageY;

    acc_append('c', currTm, x, y, windowWidth, windowHeight);
  }

  /** Pageview */
  var pageview_url = (util_ishttps() ? 'https://' : 'http://') + '@pageviewurl';
  transport_send(pageview_url, 'ts=' + (new Date()).getTime());

  /** AIAN API **/

  aian = function() { return fn_papply(aian, arguments); };
  aian.T = '';

  aian.token = function(token) {
    this.T = token;
  };

  aian.track = function() {
    util_listen(win, 'scroll', evh_scroll, false);
    util_listen(win, 'mousemove', evh_mousemove, false);
    util_listen(win, 'resize', evh_resize, false);
    util_listen(win, 'click', evh_click, false);
  };

  onready(function() {
    windowHeight = html.clientHeight;
    windowWidth = html.clientWidth;
    var oaian = win['aian'];
    if(oaian) {
      aian.V = oaian.v; // when page was visited
      var _aian = win['aian'] = aian;
      fn_setsafe(_aian, 'token', _aian.token);
      fn_setsafe(_aian, 'track', _aian.track);
      oaian.p && fn_mapply(_aian, oaian.p);
    }
  });

  /** Start Functions registered to onready */
  if(doc.readyState === 'complete' || (doc.readyState !== 'loading' && !html.doScroll)) {
    win.setTimeout(ready);
  } else {
    doc.addEventListener('DOMContentLoaded', readycompleted, false);
    win.addEventListener('load', readycompleted, false); // fallback
  }

})(window);