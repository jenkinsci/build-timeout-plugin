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

package hudson.plugins.build_timeout

import jenkins.model.Jenkins

st = namespace("jelly:stapler")

def descriptor = Jenkins.instance.getDescriptorOrDie(BuildTimeoutWrapper.class)

def myselfName = descriptor.plugin.shortName

// help file for strategy itself.
def strategyRawHelpFile = descriptor.getHelpFile("strategyRaw")

if (strategyRawHelpFile != null) {
  div(
      class: "build-timeout-nested-help",
      helpURL: String.format("%s%s", rootURL, strategyRawHelpFile),
      myselfName: myselfName,
  ) {
    text("Loading...")
  }
}

dl() {
  descriptor.strategies.each() { d ->
    def helpFile = d.getHelpFile()
    dt(d.displayName)
    if (helpFile != null) {
      dd(
          class: "build-timeout-nested-help",
          helpURL: String.format("%s%s", rootURL, helpFile),
          myselfName: myselfName,
      ) {
        text("Loading...")
      }
    } else {
      dd("No help available.")
    }
  }
}

