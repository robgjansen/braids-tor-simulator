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

Created on Mar 5, 2010
$id$
@author: rob

Parse aggregate number of requests (top level and embedded) and data transferred per node.
'''

import sys, gzip

from matplotlib import use
use('Agg')

from matplotlib.pylab import figure, savefig, show, legend, bar, xlabel, ylabel, xlim, ylim, xticks, yticks
from numpy import arange

def main():
    filenames = []
    for i in range(len(sys.argv)):
        if i == 0: continue
        filenames.append(sys.argv[i])
    
    for filename in filenames:
        web = set()
        web_total_connections = 0.0
        web_total_bytes = 0.0
        bt = set()
        bt_total_connections = 0.0
        bt_total_bytes = 0.0
        
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
                if line.find("FileSharer@") > -1 and line.find("rtt measurement:") > -1:
                    parts = line.split(' ')
                    id = parts[9]
                    id = id[id.rfind('@')+1:id.rfind('[')]
                    if id not in bt:
                        bt.add(id)
#                    bt_total_connections += 1
                    bt_total_bytes += (float(parts[13]) + float(parts[17]))
#                    bt_total += (float(parts[13]) + float(parts[17]))
                elif line.find("WebBrowser@") > -1 and line.find("rtt measurement:") > -1:
                    parts = line.split(' ')
                    id = parts[9]
                    id = id[id.rfind('@')+1:id.rfind('[')]
                    if id not in web:
                        web.add(id)
#                    web_total_connections += (float(parts[17]) + float(parts[20]))
                    web_total_bytes += (float(parts[24]) + float(parts[29]))
#                    web_total += (float(parts[24]) + float(parts[29]))
                elif line.find("generateSummary") > -1 and line.find("connections") > -1:
                    parts = line.split(' ')
                    web_total_connections = float(parts[9])
                    bt_total_connections = float(parts[13])
        
        conn_total = web_total_connections + bt_total_connections
        byte_total = web_total_bytes + bt_total_bytes
        print web_total_bytes, web_total_connections, bt_total_bytes, bt_total_connections
#        print "bt,", bt_total, len(bt), bt_total / len(bt), (bt_total / (bt_total+web_total))
#        print "web,", web_total, len(web), web_total / len(web), (web_total / (bt_total+web_total))

    figure(figsize=(6, 4))
    N = arange(4)
    width = 0.35
    p1 = bar(N, (.925,.033,.580,.400), width, color='0.9')
    p2 = bar(N+width, (web_total_connections/conn_total,bt_total_connections/conn_total,web_total_bytes/byte_total,bt_total_bytes/byte_total), width, color='0.3')
    ylabel("Percent")
    
    xticks(N+width, ('HTTP Conn.', 'BT Conn.', 'HTTP Bytes', 'BT Bytes') )
    
    legend( (p1[0], p2[0]), ('McCoy', 'BRAIDS') )
    savefig('traffic.pdf', format='pdf')
#    show()
    
if __name__ == '__main__':
    main()
