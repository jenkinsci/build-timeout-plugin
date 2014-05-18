/*
 * The MIT License
 * 
 * Copyright (c) 2014 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
Behaviour.register({".build-timeout-nested-help": function(e) {
  // ensure Behavior is applied only once.
  $(e).removeClassName("build-timeout-nested-help");
  new Ajax.Request(e.getAttribute("helpURL"), {
    method : 'get',
    onSuccess : function(x) {
      e.innerHTML = x.responseText;
      var myselfName = e.getAttribute("myselfName");
      var pluginName = x.getResponseHeader("X-Plugin-Short-Name");
      if (myselfName != pluginName) {
        var from = x.getResponseHeader("X-Plugin-From");
        if (from) {
          e.innerHTML += "<div class='from-plugin'>"+from+"</div>";
        }
      }
      layoutUpdateCallback.call();
    },
    onFailure : function(x) {
      e.innerHTML = "<b>ERROR</b>: Failed to load help file: " + x.statusText;
    }
  });
}});

/**
 * Allows run Behavior when help is loaded.
 */
layoutUpdateCallback.add(function() {
  $$(".build-timeout-nested-help").each(function(e){Behaviour.applySubtree(e, true)});
});

