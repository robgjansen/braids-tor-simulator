'''
Copyright 2010 Rob Jansen

This file is part of braids-tor-simulator.

braids-tor-simulator is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

braids-tor-simulator is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with braids-tor-simulator.  If not, see <http://www.gnu.org/licenses/>.

Created on Feb 1, 2010
$id$
@author: jansen
'''

import sys, gzip

def main():
    filenames = []
    for i in range(len(sys.argv)):
        if i == 0: continue
        filenames.append(sys.argv[i])
    
    for filename in filenames:
        web = set()
        web_total = 0.0
        bt = set()
        bt_total = 0.0
        
        if filename.find('.gz') > -1:
            file = gzip.open(filename)
        else:
            file = open(filename)
        for line in file:
            if line.startswith('#'):
                pass
            elif line.find('@') < 0:
                pass
            else:
                time = int((line.split(' '))[7])
                if time > 1800000000000: # 30 minutes
                    break
                elif line.find("FileSharer@") > -1 and line.find("rtt measurement:") > -1:
                    parts = line.split(' ')
                    id = parts[9]
                    id = id[id.rfind('@')+1:id.rfind('[')]
                    if id not in bt:
                        bt.add(id)
                    bt_total += (float(parts[13]) + float(parts[17]))
                elif line.find("WebBrowser@") > -1 and line.find("rtt measurement:") > -1:
                    parts = line.split(' ')
                    id = parts[9]
                    id = id[id.rfind('@')+1:id.rfind('[')]
                    if id not in web:
                        web.add(id)
                    web_total += (float(parts[24]) + float(parts[29]))
        
        print "bt,", bt_total, len(bt), bt_total / len(bt), (bt_total / (bt_total+web_total))
        print "web,", web_total, len(web), web_total / len(web), (web_total / (bt_total+web_total))
    
if __name__ == '__main__':
    main()