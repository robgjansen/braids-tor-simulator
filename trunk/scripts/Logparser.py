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

Created on Jan 9, 2010
$Id: Logparser.py 948 2010-10-12 03:33:57Z jansen $

@author: rob
'''

import gzip, csv#,sys
from matplotlib.pylab import figure, savefig, show, legend, plot, xlabel, ylabel, xlim, ylim
from optparse import OptionParser

usage = "usage: %prog [options] arg"
parser = OptionParser(usage)
parser.add_option("-r", "--read", dest="gzfilenames", nargs=5,
                  help="full read data from the 5 FILENAMES in order of tor-0-20-50-80, output csv file that can then be graphed")
parser.add_option("-g", "--graph", dest="csvfilename", nargs=1,
                  help="graph data from FILENAME, parsed from a full read")
parser.add_option("-l", "--legacy", dest="filename",
                  help="read data from a single FILENAME and graph immediately")
parser.add_option("-s", "--save", action="store_true", dest="save_figs", default=False, 
                  help="save figures if they are generated")
parser.add_option("-v", "--noview", action="store_false", dest="view_figs", default=True, 
                  help="do NOT show figures if they are generated")

(options, args) = parser.parse_args()
if len(args) != 0:
    parser.error("incorrect number of arguments")
    
# simulation warmup time - 10 minutes
warmup = 0#600000

bt_rtt_d = {}
web_rtt_d = {}
delay_d = {}
heartbeat_l = []

current_minute = 0;

def parse_to_csv():
    
    #import files
    tor = options.gzfilenames[0]
    zero = options.gzfilenames[1]
    twenty = options.gzfilenames[2]
    fifty = options.gzfilenames[3]
    eighty = options.gzfilenames[4]
    
    # parse all data, use temp for data we do not need
    read_file(tor)
    tor_rtt_x = get_web_rtt_CDF("NormalData")
    temp, tor_util_x, temp, temp = get_bt_rtt_CDF_weighted()
    
    x, tor_memory_y = get_memory_over_time()
    temp, tor_dgs_total_y, temp, temp, temp = get_datagrams_over_time()
    temp, tor_mgs_total_y, temp, temp, temp = get_messages_over_time()
    
    read_file(zero)
    zero_rtt_normal_x = get_web_rtt_CDF("NormalData")
    zero_rtt_lowlatency_x = get_web_rtt_CDF("LowLatencyData")
    temp, zero_util_btclient_x, temp, zero_util_btrelay_x = get_bt_rtt_CDF_weighted()

    read_file(twenty)
    twenty_rtt_normal_x = get_web_rtt_CDF("NormalData")
    twenty_rtt_lowlatency_x = get_web_rtt_CDF("LowLatencyData")
    temp, twenty_util_btclient_x, temp, twenty_util_btrelay_x = get_bt_rtt_CDF_weighted()

    read_file(fifty)
    fifty_rtt_normal_x = get_web_rtt_CDF("NormalData")
    fifty_rtt_lowlatency_x = get_web_rtt_CDF("LowLatencyData")
    temp, fifty_util_btclient_x, temp, fifty_util_btrelay_x = get_bt_rtt_CDF_weighted()
    
    temp, fifty_memory_y = get_memory_over_time()
    temp, fifty_dgs_total_y, fifty_dgs_ht_y, fifty_dgs_ll_y, fifty_dgs_n_y = get_datagrams_over_time()
    temp, fifty_mgs_total_y, fifty_mgs_ht_y, fifty_mgs_ll_y, fifty_mgs_n_y = get_messages_over_time()

    read_file(eighty)
    eighty_rtt_normal_x = get_web_rtt_CDF("NormalData")
    eighty_rtt_lowlatency_x = get_web_rtt_CDF("LowLatencyData")
    temp, eighty_util_btclient_x, temp, eighty_util_btrelay_x = get_bt_rtt_CDF_weighted()

    #free the memory
    reset()
    
    # output graph data into csv to avoid parsing it again
    writer = csv.writer(open('graph_data.csv', 'w'))
    writer.writerows([tor_rtt_x, tor_util_x])
    writer.writerows([zero_rtt_normal_x, zero_rtt_lowlatency_x, zero_util_btclient_x, zero_util_btrelay_x])
    writer.writerows([twenty_rtt_normal_x, twenty_rtt_lowlatency_x, twenty_util_btclient_x, twenty_util_btrelay_x])
    writer.writerows([fifty_rtt_normal_x, fifty_rtt_lowlatency_x, fifty_util_btclient_x, fifty_util_btrelay_x])
    writer.writerows([eighty_rtt_normal_x, eighty_rtt_lowlatency_x, eighty_util_btclient_x, eighty_util_btrelay_x])
    
    writer.writerows([x, tor_memory_y, fifty_memory_y])
    writer.writerows([tor_dgs_total_y, fifty_dgs_total_y, fifty_dgs_ht_y, fifty_dgs_ll_y, fifty_dgs_n_y])
    writer.writerows([tor_mgs_total_y, fifty_mgs_total_y, fifty_mgs_ht_y, fifty_mgs_ll_y, fifty_mgs_n_y])

#config for how many markers to show
# (start_index, stride)
util_markevery_all = (0, 500)
util_markevery_one = (0, 200)
rtt_markevery = (0, 10000)
marker_size = 5
line_width = 2.0
mark_every = (0, 0)
def graph_from_csv():
    global mark_every
    # properties for the graphs, keep consistent
    # label, linestyle, color, marker
    tor_props = ['Tor', '-', 'k', 'None']
    zero_props = ['0%', '--', 'r', '^']
    twenty_props = ['20%', ':', 'b', 's']
    fifty_props = ['50%', '--', 'g', 'o']
    eighty_props = ['80%', ':', 'm', '*']
    
    #grab already parsed data from the csv
    reader = csv.reader(open(options.csvfilename, "rb"))
    tor_rtt_x = to_float(reader.next())
    tor_util_x = to_float(reader.next())
    zero_rtt_normal_x = to_float(reader.next())
    zero_rtt_lowlatency_x = to_float(reader.next())
    zero_util_btclient_x = to_float(reader.next())
    zero_util_btrelay_x = to_float(reader.next())
    twenty_rtt_normal_x = to_float(reader.next())
    twenty_rtt_lowlatency_x = to_float(reader.next())
    twenty_util_btclient_x = to_float(reader.next())
    twenty_util_btrelay_x = to_float(reader.next())
    fifty_rtt_normal_x = to_float(reader.next())
    fifty_rtt_lowlatency_x = to_float(reader.next())
    fifty_util_btclient_x = to_float(reader.next())
    fifty_util_btrelay_x = to_float(reader.next())
    eighty_rtt_normal_x = to_float(reader.next())
    eighty_rtt_lowlatency_x = to_float(reader.next())
    eighty_util_btclient_x = to_float(reader.next())
    eighty_util_btrelay_x = to_float(reader.next())
    
    time = to_float(reader.next())
    tor_memory = to_float(reader.next())
    fifty_memory = to_float(reader.next())
    tor_dgs_total = to_float(reader.next())
    fifty_dgs_total_y = to_float(reader.next())
    fifty_dgs_ht_y = to_float(reader.next())
    fifty_dgs_ll_y = to_float(reader.next())
    fifty_dgs_n_y = to_float(reader.next())
    tor_mgs_total = to_float(reader.next())
    fifty_mgs_total_y = to_float(reader.next())
    fifty_mgs_ht_y = to_float(reader.next())
    fifty_mgs_ll_y = to_float(reader.next())
    fifty_mgs_n_y = to_float(reader.next())
    
    #now draw the graphs
    mark_every = rtt_markevery
    
    #rtt low latency
    figure(figsize=(6, 4))
    do_plot_CDF(tor_rtt_x, tor_props)
    do_plot_CDF(zero_rtt_lowlatency_x, zero_props)
    do_plot_CDF(twenty_rtt_lowlatency_x, twenty_props)
    do_plot_CDF(fifty_rtt_lowlatency_x, fifty_props)
    do_plot_CDF(eighty_rtt_lowlatency_x, eighty_props)
    ylim(0, 1)
    xlim(0, 10)
    ylabel("Cumulative Fraction")
    xlabel("Paid Webpage Download Time (s)")
    do_legend2()
    if options.save_figs: savefig('ll_rtt.png')

    #rtt normal
    figure(figsize=(6, 4))
    do_plot_CDF(tor_rtt_x, tor_props)
    do_plot_CDF(zero_rtt_normal_x, zero_props)
    do_plot_CDF(twenty_rtt_normal_x, twenty_props)
    do_plot_CDF(fifty_rtt_normal_x, fifty_props)
    do_plot_CDF(eighty_rtt_normal_x, eighty_props)
    ylim(0, 1)
    xlim(0, 10)
    ylabel("Cumulative Fraction")
    xlabel("Unpaid Webpage Download Time (s)")
    do_legend2()
    if options.save_figs: savefig('n_rtt.png')
    
    mark_every = util_markevery_all
    
    #util high throughput
    figure(figsize=(6, 4))
    do_plot_CDF(tor_util_x, tor_props)
    #do_plot_CDF(zero_util_btrelay_x, zero_props)
    do_plot_CDF(twenty_util_btrelay_x, twenty_props)
    do_plot_CDF(fifty_util_btrelay_x, fifty_props)
    do_plot_CDF(eighty_util_btrelay_x, eighty_props)
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Relay Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: savefig('btrelay_util.png')
    
    #util normal
    figure(figsize=(6, 4))
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(zero_util_btclient_x, zero_props)
    do_plot_CDF(twenty_util_btclient_x, twenty_props)
    do_plot_CDF(fifty_util_btclient_x, fifty_props)
    do_plot_CDF(eighty_util_btclient_x, eighty_props)
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Client Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: savefig('btclient_util.png')
    
    mark_every = util_markevery_one
    
    figure(figsize=(4.5, 3))
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(twenty_util_btrelay_x, get_btrelay_props(twenty_props))
    do_plot_CDF(twenty_util_btclient_x, get_btclient_props(twenty_props))
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: savefig('20_util.png')
    
    figure(figsize=(4.5, 3))
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(fifty_util_btrelay_x, get_btrelay_props(fifty_props))
    do_plot_CDF(fifty_util_btclient_x, get_btclient_props(fifty_props))
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: savefig('50_util.png')
    
    figure(figsize=(4.5, 3))
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(eighty_util_btrelay_x, get_btrelay_props(eighty_props))
    do_plot_CDF(eighty_util_btclient_x, get_btclient_props(eighty_props))
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: savefig('80_util.png')
    
    # messages, datagrams, and memory over time
    figure(figsize=(6, 4))
    do_plot(time, tor_memory, tor_props)
    do_plot(time, fifty_memory, fifty_props)
    ylabel("Memory (MB)")
    xlabel("Time (m)")
    do_legend()
    if options.save_figs: savefig('memory.png')
    
    figure(figsize=(6, 4))
    plot(time, tor_mgs_total, lw=line_width, label=tor_props[0])
    plot(time, fifty_mgs_total_y, lw=line_width, label=fifty_props[0] + " total")
    plot(time, fifty_mgs_ll_y, lw=line_width, label=fifty_props[0] + " LL")
    plot(time, fifty_mgs_ht_y, lw=line_width, label=fifty_props[0] + " HT")
    plot(time, fifty_mgs_n_y, lw=line_width, label=fifty_props[0] + " N")
    ylabel("Number of Messages")
    xlabel("Time (m)")
    do_legend()
    if options.save_figs: savefig('messages.png')
    
    figure(figsize=(6, 4))
    plot(time, tor_dgs_total, lw=line_width, label=tor_props[0])
    plot(time, fifty_dgs_total_y, lw=line_width, label=fifty_props[0] + " total")
    plot(time, fifty_dgs_ll_y, lw=line_width, label=fifty_props[0] + " LL")
    plot(time, fifty_dgs_ht_y, lw=line_width, label=fifty_props[0] + " HT")
    plot(time, fifty_dgs_n_y, lw=line_width, label=fifty_props[0] + " N")
    ylabel("Number of Datagrams")
    xlabel("Time (m)")
    do_legend()
    if options.save_figs: savefig('datagrams.png')
    
    if options.view_figs: show()

def to_float(list):
    new = []
    for item in list:
        new.append(float(item))
    return new

# this is for graphs that show both btrelay and btclient data with the same percentage of converters
def get_btrelay_props(props):
    new = []
    new.append(props[0] + ", FSR")
    new.append('--')
    new.append('b')
    new.append('x')
    return new

# this is for graphs that show both btrelay and btclient data with the same percentage of converters
def get_btclient_props(props):
    new = []
    new.append(props[0] + ", FSC")
    new.append(':')
    new.append('r')
    new.append('+')
    return new
    
def do_plot_CDF(x, props):
    do_plot(x, get_y_cdf_axis(x), props)
    
def do_plot(x, y, props):
    ew = 1.0
    if props[3] == '+' or props[3] == 'x':
        ew = 1.5
    plot(x, y, markevery=mark_every, ms=marker_size, mew=ew, lw=line_width, label=props[0], ls=props[1], c=props[2], marker=props[3])
    
def get_y_cdf_axis(x):
    y = []
    frac = 0
    for i in xrange(len(x)):
        frac += 1.0 / float(len(x))
        y.append(frac)
    return y

def do_legend():
    # see http://matplotlib.sourceforge.net/users/legend_guide.html
    leg = legend(bbox_to_anchor=(0., 1.02, 1., .102), loc=3, ncol=5, mode="expand", borderaxespad=0., numpoints=1, handletextpad=0.2)
    if leg is not None:
        for t in leg.get_texts():
            t.set_fontsize('small')
    do_grid()
            
def do_legend2():
    # see http://matplotlib.sourceforge.net/users/legend_guide.html
    leg = legend(loc='lower right', ncol=1, borderaxespad=0.5, numpoints=1, handletextpad=0.2)
    if leg is not None:
        for t in leg.get_texts():
            t.set_fontsize('small')
    do_grid()

def do_grid():
    from matplotlib.pylab import gca
    gca().yaxis.grid(True, c='0.5')
    gca().xaxis.grid(True, c='0.5')
         
def main():
    plots1 = []
    plots2 = []
    plots3 = []
    plots4 = []
    plots5 = []
    plots6 = []
    
    yl1 = 'Cumulative fraction'
    yl2 = 'Java VM Memory (MB)'
    yl3 = 'Total Datagrams'
    yl4 = 'Total Messages'
    xl1 = 'Average BT goodput (kbps)'
    xl2 = 'BT utilization (%)'
    xl3 = 'Average WEB rtt (s)'
    xl4 = 'Time (m)'
    
    ps = [("LowLatencyData", "LL"), ("HighThroughputData", "HT"), ("NormalData", "N")]

    filename = options.filename
    label = "test"
    read_file(filename)
    
    for p in ps:
        x = get_web_rtt_CDF(p[0])
        if(len(x) > 0):
            plots1.append((x, get_y_cdf_axis(x), label + " " + p[1]))
    
#        for p in ps:
#            x, xutil, y = get_bt_rtt_CDF(p[0])
#            if(len(x) > 0):
#                plots2.append((x, y, label + " " + p[1]))
#                plots3.append((xutil, y, label + " " + p[1]))

    x_norm, xutil_norm, x_conv, xutil_conv = get_bt_rtt_CDF_weighted()
    if len(x_norm) > 0:
        plots2.append((x_norm, get_y_cdf_axis(x_norm), label + " N"))
        plots3.append((xutil_norm, get_y_cdf_axis(xutil_norm), label + " N"))
    if len(x_conv) > 0:
        plots2.append((x_conv, get_y_cdf_axis(x_conv), label + " C"))
        plots3.append((xutil_conv, get_y_cdf_axis(xutil_conv), label + " C"))
            
    x, y = get_memory_over_time()
    plots4.append((x, y, label))
    
    x, y1, y2, y3, y4 = get_datagrams_over_time()
    plots5.append((x, y1, label + " total"))
    plots5.append((x, y2, label + " HT"))
    plots5.append((x, y3, label + " LL"))
    plots5.append((x, y4, label + " N"))
    
    x, y1, y2, y3, y4 = get_messages_over_time()
    plots6.append((x, y1, label + " total"))
    plots6.append((x, y2, label + " HT"))
    plots6.append((x, y3, label + " LL"))
    plots6.append((x, y4, label + " N"))
    
    draw(plots1, xl3, yl1, True, "rtt")
    draw(plots2, xl1, yl1, True, "goodput")
    draw(plots3, xl2, yl1, True, "util")
    draw(plots4, xl4, yl2, False, "memory")
    draw(plots5, xl4, yl3, False, "datagrams")
    draw(plots6, xl4, yl4, False, "messages")
    if options.view_figs: show()

def reset():
    global  bt_rtt_d, web_rtt_d, delay_d, heartbeat_l
    del(bt_rtt_d)
    bt_rtt_d = {}
    del(web_rtt_d)
    web_rtt_d = {}
    del(delay_d)
    delay_d = {}
    del(heartbeat_l)
    heartbeat_l = []

def read_file(filename):
    reset()
    print "Reading", filename
    if(filename.find('.gz') > -1):
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
            if time < warmup:
                pass
            elif line.find("Heartbeat") > -1:
                read_heartbeat(line)
            elif line.find("FileSharer@") > -1 and line.find("rtt measurement:") > -1:
                read_bt_rtt(line, time)
            elif line.find("WebBrowser@") > -1 and line.find("rtt measurement:") > -1:
                read_web_rtt(line, time)
    print "Parsing", filename
    
def read_bt_rtt(line, time):
    parts = line.split(' ')
    t = parts[9]
    id = t[t.rfind('@') + 1:t.rfind('[')]
    bw = t[t.rfind('/') + 1:t.rfind('k')]
    app = parts[9][:parts[9].rfind('@')]
    priority = parts[10]
    bytes = int(parts[13]) + int(parts[17])
    rtt = parts[21]
    if(id not in bt_rtt_d):
        bt_rtt_d[id] = []
    bt_rtt_d[id].append((int(time), int(bytes), float(bw), int(rtt), priority, app))

def read_web_rtt(line, time):
    parts = line.split(' ')
    id = parts[9]
    id = id[id.rfind('@') + 1:id.rfind('[')]
    priority = parts[10]
    rtt = parts[13]
    if(id not in web_rtt_d):
        web_rtt_d[id] = []
    web_rtt_d[id].append((int(time), int(rtt), priority))

def read_heartbeat(line):
    global current_minute
    parts = line.split(' ')
    time = parts[10]
    time = int(time[:time.find('/')])
    mem = parts[12]
    mem = int(mem[:mem.find('M')])
    m = int(parts[13])
    mht = int(parts[15])
    mll = int(parts[17])
    mn = int(parts[19])
    d = int(parts[21])
    dht = int(parts[23])
    dll = int(parts[25])
    dn = int(parts[27])
    heartbeat_l.append((time, mem, m, mht, mll, mn, d, dht, dll, dn))
    current_minute = time
    
def get_web_rtt_CDF(priority):
    x = []
    frac = 0
    for key in web_rtt_d:
        tuplelist = web_rtt_d[key]
        total = 0
        count = 0
        for entry in tuplelist:
            if entry[2] == priority:
                total += entry[1]
                count += 1
        if count > 0:
            x.append((total / count) / 1000.0)
    x.sort()
    return x

def get_bt_rtt_CDF(priority):
    x = []
    xutil = []
    y = []
    frac = 0
    for key in bt_rtt_d:
        tuplelist = bt_rtt_d[key]
        total = 0
        start = 0
        end = 0
        bw = 0
        for entry in tuplelist:
            if entry[4] == priority:
                if start == 0:
                    start = entry[0] - entry[3]
                    bw = float(2.0 * entry[2])
                end = entry[0]
                total += entry[1]
        if bw > 0:
            kbps = float(total * 8.0 / 1000.0) / ((end - start) / 1000000000.0)
            x.append(kbps)
            xutil.append((kbps / bw) * 100.0)
    for i in range(len(x)):
        frac += 1.0 / float(len(x))
        y.append(frac)
    x.sort()
    xutil.sort()
    return x, xutil, y

def get_bt_rtt_CDF_weighted():
    x_norm = []
    xutil_norm = []
    x_conv = []
    xutil_conv = []
    for key in bt_rtt_d:
        tuplelist = bt_rtt_d[key]
        if tuplelist[0][5].find('FileSharer@Client') > -1:
            kbps, kbps_percent = get_bt_helper(tuplelist)
            if kbps > -1:
                x_norm.append(kbps)
                xutil_norm.append(kbps_percent)
        elif tuplelist[0][5].find('FileSharer@Relay') > -1:
            kbps, kbps_percent = get_bt_helper(tuplelist)
            if kbps > -1:
                x_conv.append(kbps)
                xutil_conv.append(kbps_percent)
        
    x_norm.sort()
    xutil_norm.sort()
    x_conv.sort()
    xutil_conv.sort()
    return x_norm, xutil_norm, x_conv, xutil_conv

def get_bt_helper(tuplelist):
    bw = 0
    start = 0
    end = 0
    total = 0
    num = 0
    for entry in tuplelist:
        if start == 0:
            start = entry[0] - entry[3]
            bw = float(2.0 * entry[2])
        end = entry[0]
        total += entry[1]
        num += 1
    kbps = -1 
    kbps_percent = -1
    if bw > 0:
        if num > 0: 
            kbps = float(total * 8.0 / 1000.0) / ((end - start) / 1000000000.0)
            kbps_percent = (kbps / bw) * 100.0
    return kbps, kbps_percent
 
def get_memory_over_time():
    x = []
    y = []
    for item in heartbeat_l:
        x.append(item[0])
        y.append(item[1])
    return x, y

def get_messages_over_time():
    x = []
    y1 = []
    y2 = []
    y3 = []
    y4 = []
    for item in heartbeat_l:
        x.append(item[0])
        y1.append(item[2])#total
        y2.append(item[3])#ht
        y3.append(item[4])#ll
        y4.append(item[5])#n
    return x, y1, y2, y3, y4

def get_datagrams_over_time():
    x = []
    y1 = []
    y2 = []
    y3 = []
    y4 = []
    for item in heartbeat_l:
        x.append(item[0])
        y1.append(item[6])#total
        y2.append(item[7])#ht
        y3.append(item[8])#ll
        y4.append(item[9])#n
    return x, y1, y2, y3, y4

def draw(plots, xaxislabel, yaxislabel, isCDF, savename):
    figure(figsize=(6, 4))
    for p in plots:
        plot(p[0], p[1], linewidth=3.0, label=p[2])
    if isCDF:
        ylim(0, 1)
    ylabel(yaxislabel)
    xlabel(xaxislabel)
    
    do_legend()
    if options.save_figs: savefig(savename)
        
def draw_relay_bandwidth_CDF():
    file = open("../src/main/resources/relay_bandwidth.dat")
    
    x = []
    y = []
    bytes_to_kbits = 8.0 / 1000.0
    
    for line in file:
        if line.startswith('#'):
            pass
        else:
            l = line.strip().split(' ')
            x.append(float(l[0]) * bytes_to_kbits)
            y.append(float(l[1]))

    figure(figsize=(6, 4))
    plot(x, y, linewidth=3.0)
    ylabel('Cumulative fraction (%)')
    xlabel('Relay bandwidth (kbps)')
    
    leg = legend(loc='lower right', ncol=1, columnspacing=0.03, handletextpad=0.01)
    #for t in leg.get_texts():
    #    t.set_fontsize('small')

if __name__ == '__main__':
    #draw_relay_bandwidth_CDF()
    if options.gzfilenames is not None:
        print "reading from gzs..."
        parse_to_csv()
    if options.csvfilename is not None:
        print "graphing csv..."
        graph_from_csv()
    if options.filename is not None:
        print "reading single..."
        main()
    print "done!"
        
