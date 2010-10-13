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

Created on Feb 18, 2010
$Id: ParallelLogParser.py 948 2010-10-12 03:33:57Z jansen $
@author: rob
'''

import gzip, csv#,sys
from optparse import OptionParser

usage = "usage: %prog [options] args"
parser = OptionParser(usage)
parser.add_option("-p", "--parse", action="store_true", dest="do_parse", default=False,
                  help="parse data from arg FILENAMES, output csv file that can then be graphed. 5 input args should be given as: tor braids_0 braids_20 braids_50 braids_80")
parser.add_option("-m", "--multiprocessing", action="store_true", dest="do_parallel", default=False,
                  help="use multiprocessing when parsing")
parser.add_option("-g", "--graph", action="store_true", dest="do_graph", default=False,
                  help="graph data from arg FILENAMES previously parsed")
parser.add_option("-a", "--all", action="store_true", dest="graph_all", default=False,
                  help="graph all webpage and download data from any number of input FILENAMES previously parsed")
parser.add_option("-s", "--save", action="store_true", dest="save_figs", default=False, 
                  help="save figures if they are generated")
parser.add_option("-x", "--noview", action="store_false", dest="view_figs", default=True, 
                  help="do NOT show figures if they are generated")

(options, args) = parser.parse_args()
    
# simulation warmup time - 10 minutes
warmup = 0#600000

def parse(filename):
    import operator
    print "Reading", filename, "..."
    heartbeat_l = []
    fsr_d, fsc_d = {}, {}
    wcll_s, wcn_s = set(), set()
    
    wcn_x, wcll_x, fsht_x, fsn_x = [], [], [], []
    
    bandwidth_l = []
    totalweb = 0
    for line in open_log(filename):
        if line.startswith('#'):
            pass
        elif line.find('@') < 0:
            pass
        else:
            parts = line.split(' ')
            time = int(parts[7])
            id = parts[9][parts[9].rfind('@') + 1:parts[9].rfind('(')]
            if time < warmup:
                break
            elif line.find("WebBrowser@") > -1:
                rtt = float(parts[13])
                s = {}
                if parts[10] == "NormalData": 
                    wcn_x.append(rtt/1000.0)
                    s = wcn_s
                elif parts[10] == "LowLatencyData":
                    wcll_x.append(rtt/1000.0)
                    s = wcll_s
                else:
                    print "WebBrowser not ll or n:", line 
                    continue
                if id not in wcn_s and id not in wcll_s: 
                    totalweb += 1
                    if line.find("WEBRELAY") > -1 or line.find("WEBEXITRELAY") > -1: bandwidth_l.append(getBandwidthTuple(line, True))
                if id not in s: s.add(id)
            elif line.find("FileSharer@") > -1:
                # first keep track of aggregate throughput for each node
                d = {}
                if line.find("FSCLIENT") > -1: d = fsc_d
                elif line.find("FSRELAY") > -1 or line.find("FSEXITRELAY") > -1: 
                    d = fsr_d
                    if id not in d: bandwidth_l.append(getBandwidthTuple(line, False))
                else:
                    print "FileSharer not client or relay:", line 
                    continue
                if id not in d:
                    bwparts = parts[9].split('[')
                    if len(bwparts) > 4: bandwidth = 2.0 * (float(bwparts[2][:bwparts[2].find('k')]) + float(bwparts[4][:bwparts[4].find('k')]))
                    else: bandwidth = 2.0 * float(bwparts[2][:bwparts[2].find('k')])
                        
                    start = int(parts[7]) - int(parts[21])
                    d[id] = [bandwidth, start, 0.0, 0.0] #end, totalbytes
                tuple = d[id]
                tuple[2] = int(parts[7])
                bytes = int(parts[13]) + int(parts[17])
                tuple[3] += bytes
                
                # also keep track of individual stats per request per class
                rtt = float(parts[21])
                #bw as util for this single request
                kbits = bytes * 8.0 / 1000.0
                secs = rtt/1000.0
                kbps = kbits/secs
                kbps_perc = (kbps/tuple[0]) * 100
                if parts[10] == "HighThroughputData": 
                    fsht_x.append(kbps)
                elif parts[10] == "NormalData":
                    fsn_x.append(kbps)
                else:
                    print "FileSharer not ht or n:", line 
                    continue
                
            elif line.find("Heartbeat") > -1:
                heartbeat_l = read_heartbeat(line, filename, heartbeat_l)
                
#    print "Found", totalweb, "WebBrowsers and", len(fsr_d) + len(fsc_d), "FileSharers"
    
    fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x = [], [], [], []
    totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y = [], [], [], [], [], [], [], [], [], []
    bwDown_y, bwUp_y, bwCon_y = [], [], []
    
    # web clients
    wcn_x.sort()
    wcll_x.sort()
    
    fsht_x.sort()
    fsn_x.sort()
    
    for id in fsr_d:
        tuple = fsr_d[id]
        kbps = float(tuple[3] * 8.0 / 1000.0) / ((tuple[2] - tuple[1]) / 1000000000.0)
        fsrkbps_x.append(kbps)
        fsrutil_x.append((kbps / tuple[0]) * 100.0)
    fsrkbps_x.sort()
    fsrutil_x.sort()
    for id in fsc_d:
        tuple = fsc_d[id]
        kbps = float(tuple[3] * 8.0 / 1000.0) / ((tuple[2] - tuple[1]) / 1000000000.0)
        fsckbps_x.append(kbps)
        fscutil_x.append((kbps / tuple[0]) * 100.0)
    fsckbps_x.sort()
    fscutil_x.sort()
        
    # memory, messages, datagrams
    for item in heartbeat_l:
        totaltime.append(item[0])
        mem_y.append(item[1]) # memory
        msgt_y.append(item[2]) # msg total
        msght_y.append(item[3]) # msg ht
        msgll_y.append(item[4]) # msg ll
        msgn_y.append(item[5]) # msg n
        datt_y.append(item[6]) # dat total
        datht_y.append(item[7]) # dat ht
        datll_y.append(item[8]) # dat ll
        datn_y.append(item[9]) # dat n
    
    # sort bandwidth by contributed, tuple form is [down, up, contributed]
    temp = sorted(bandwidth_l, key=operator.itemgetter(2))
    bwDown_y = map(operator.itemgetter(0), temp)
    bwUp_y = map(operator.itemgetter(1), temp)
    bwCon_y = map(operator.itemgetter(2), temp)
    
    return wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x

def getBandwidthTuple(line, isWeb):
    parts = line.split()[9].split('[')
    # down, up, contributed
    down = float(parts[1][:parts[1].find('k')])
    up = float(parts[2][:parts[2].find('k')])
    con = float(parts[3][:parts[3].find('k')])
    tuple = (down+con, up+con, con)
    return tuple

def open_log(filename):
    import os.path
    if os.path.exists(filename):
        if(filename.find('.gz') > -1):
            return gzip.open(filename)
        else:
            return open(filename)
    else:
        return None
    
def read_heartbeat(line, filename, list):
    parts = line.split(' ')
    time = parts[10]
    time = int(time[:time.find('/')])
    print filename, "Heartbeat", time
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
    list.append((time, mem, m, mht, mll, mn, d, dht, dll, dn))
    return list

desc = "Row Order: wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x"
def parse_all_args():
    # parse, and output parsed data into csv to avoid parsing it again
    for filename in args:
        parse_to_csv(filename)

def parallel_parse():
    from multiprocessing import Pool, cpu_count
    pool = Pool(processes=cpu_count())
    for filename in args:
        pool.apply_async(parse_to_csv, [filename])
    pool.close()
    pool.join()

def parse_to_csv(filename):
    # parse, then write to csv
    if(filename.rfind('.log') > -1):
        wfile = filename[:filename.rfind('.log')]+'.dat.gz'
    else:
        wfile = filename[:filename.rfind('.')]+'.dat.gz'
    writer = csv.writer(gzip.open(wfile, 'w'))
    wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x = parse(filename)
    
    writer.writerow(["#",filename, desc])
    writer.writerows([wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x])

index = 0
def read_next():
    global index
    if index >= len(args):
        return [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], []
    else: 
        index += 1
        return read_dat(args[index-1])

def read_dat(filename):
    if(filename.find("dat") > -1):
        file = open_log(filename)
    else:
        file = open_log(filename[:filename.rfind('.')]+'.dat.gz')
    if file is None:
        return [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], []
    
    #grab already parsed data from the csv
    reader = csv.reader(file)
    header = reader.next()
    wcn_x = to_float(reader.next())
    wcll_x = to_float(reader.next()) 
    fsrkbps_x = to_float(reader.next()) 
    fsrutil_x = to_float(reader.next()) 
    fsckbps_x = to_float(reader.next()) 
    fscutil_x = to_float(reader.next()) 
    totaltime = to_float(reader.next()) 
    mem_y = to_float(reader.next()) 
    msgt_y = to_float(reader.next()) 
    msght_y = to_float(reader.next()) 
    msgll_y = to_float(reader.next()) 
    msgn_y = to_float(reader.next()) 
    datt_y = to_float(reader.next()) 
    datht_y = to_float(reader.next()) 
    datll_y = to_float(reader.next()) 
    datn_y = to_float(reader.next())
    bwDown_y = to_float(reader.next())
    bwUp_y = to_float(reader.next())
    bwCon_y = to_float(reader.next())
    fsht_x = to_float(reader.next())
    fsn_x = to_float(reader.next())
    
    return wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x

#config for how many markers to show
# (start_index, stride)
mark_every_setting=(1,1)
num_markers=11
marker_size = 5
line_width = 2.0
def graph_from_csv():
    if not options.view_figs:
        from matplotlib import use
        use('Agg')
    from matplotlib.pylab import figure, savefig, show, legend, plot, fill_between, xlabel, ylabel, xlim, ylim, gca, close
    global mark_every_setting
    # properties for the graphs, keep consistent
    # label, linestyle, color, marker
    tor_props = ['Tor', '-', 'k', 'None']
    zero_props = ['0%', '--', 'r', '^']
    twenty_props = ['20%', ':', 'b', 's']
    fifty_props = ['50%', '--', 'g', 'o']
    eighty_props = ['80%', ':', 'm', '*']
    
    print "reading tor"
    wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x = read_next()
    
    tor_rtt_x = wcn_x
    tor_util_x = fscutil_x
    tor_client_good = fsckbps_x
    tor_util2_x = fsn_x
    time = totaltime
    tor_memory = mem_y
    tor_dgs_total = datt_y
    tor_mgs_total = msgt_y
    
    print "reading 0"
    wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x = read_next()
    
    zero_rtt_normal_x = wcn_x
    zero_rtt_lowlatency_x = wcll_x
    zero_util_btclient_x = fscutil_x
    zero_util_btrelay_x = fsrutil_x
    zero_client_good = fsckbps_x
    zero_relay_good = fsrkbps_x
    
    print "reading 20"
    wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x = read_next()
    
    twenty_rtt_normal_x = wcn_x
    twenty_rtt_lowlatency_x = wcll_x
    twenty_util_btclient_x = fscutil_x
    twenty_util_btrelay_x = fsrutil_x
    twenty_client_good = fsckbps_x
    twenty_relay_good = fsrkbps_x

    print "reading 50"
    wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x = read_next()
    
    fifty_rtt_normal_x = wcn_x
    fifty_rtt_lowlatency_x = wcll_x
    fifty_util_btclient_x = fscutil_x
    fifty_util_btrelay_x = fsrutil_x
    fifty_memory = mem_y
    fifty_dgs_total_y = datt_y
    fifty_dgs_ht_y = datht_y
    fifty_dgs_ll_y = datll_y
    fifty_dgs_n_y = datn_y
    fifty_mgs_total_y = msgt_y
    fifty_mgs_ht_y = msght_y
    fifty_mgs_ll_y = msgll_y
    fifty_mgs_n_y = msgn_y
    fifty_bwDown_y = bwDown_y
    fifty_bwUp_y = bwUp_y
    fifty_bwCon_y = bwCon_y
    fifty_fsht_x = fsht_x
    fifty_fsn_x = fsn_x
    fifty_client_good = fsckbps_x
    fifty_relay_good = fsrkbps_x

    print "reading 80"    
    wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x = read_next()
    
    eighty_rtt_normal_x = wcn_x
    eighty_rtt_lowlatency_x = wcll_x
    eighty_util_btclient_x = fscutil_x
    eighty_util_btrelay_x = fsrutil_x
    eighty_client_good = fsckbps_x
    eighty_relay_good = fsrkbps_x
    
    m = len(tor_rtt_x)/num_markers
    mark_every_setting=(m,m)
    
    print "graphing lowlatency rtt"
    #rtt low latency
    figure(figsize=(6, 4))
    do_plot_CDF(tor_rtt_x, tor_props)
    do_plot_CDF(zero_rtt_lowlatency_x, zero_props)
    do_plot_CDF(twenty_rtt_lowlatency_x, twenty_props)
    do_plot_CDF(fifty_rtt_lowlatency_x, fifty_props)
    do_plot_CDF(eighty_rtt_lowlatency_x, eighty_props)
    ylim(-0.01, 1.01)
    xlim(0, 15)
    ylabel("Cumulative Fraction")
    xlabel("Paid Webpage Download Time (s)")
    do_legend2()
    if options.save_figs: do_savefig('ll_rtt.pdf')
    if not options.view_figs: close()
    del(zero_rtt_lowlatency_x)
    del(twenty_rtt_lowlatency_x)
    del(fifty_rtt_lowlatency_x)
    del(eighty_rtt_lowlatency_x)

    print "graphing normal rtt"
    #rtt normal
    figure(figsize=(6, 4))
    do_plot_CDF(tor_rtt_x, tor_props)
    do_plot_CDF(zero_rtt_normal_x, zero_props)
    do_plot_CDF(twenty_rtt_normal_x, twenty_props)
    do_plot_CDF(fifty_rtt_normal_x, fifty_props)
    do_plot_CDF(eighty_rtt_normal_x, eighty_props)
    ylim(-0.01, 1.01)
    xlim(0, 15)
    ylabel("Cumulative Fraction")
    xlabel("Unpaid Webpage Download Time (s)")
    do_legend2()
    if options.save_figs: do_savefig('n_rtt.pdf')
    if not options.view_figs: close()
    del(zero_rtt_normal_x)
    del(twenty_rtt_normal_x)
    del(fifty_rtt_normal_x)
    del(eighty_rtt_normal_x)
    del(tor_rtt_x)
    
    m = len(tor_util_x)/num_markers
    mark_every_setting=(m,m)
    
    print "graphing util percent"
    #util high throughput
    figure(figsize=(6, 4))
    do_plot_CDF(tor_util_x, tor_props)
    #do_plot_CDF(zero_util_btrelay_x, zero_props)
    do_plot_CDF(twenty_util_btrelay_x, twenty_props)
    do_plot_CDF(fifty_util_btrelay_x, fifty_props)
    do_plot_CDF(eighty_util_btrelay_x, eighty_props)
    ylim(-0.01, 1.01)
    xlim(7, 14)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Relay Bandwidth Utilization (%)")
    do_legend2()
    if options.save_figs: do_savefig('btrelay_aggregate_util.pdf')
    if not options.view_figs: close()

    #util normal
    figure(figsize=(6, 4))
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(zero_util_btclient_x, zero_props)
    do_plot_CDF(twenty_util_btclient_x, twenty_props)
    do_plot_CDF(fifty_util_btclient_x, fifty_props)
    do_plot_CDF(eighty_util_btclient_x, eighty_props)
    ylim(-0.01, 1.01)
    xlim(5, 12)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Client Bandwidth Utilization (%)")
    do_legend2()
    if options.save_figs: do_savefig('btclient_aggregate_util.pdf')
    if not options.view_figs: close()

    figure(figsize=(4.5, 3)) #4.5, 3
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(twenty_util_btrelay_x, get_btrelay_props(twenty_props))
    do_plot_CDF(twenty_util_btclient_x, get_btclient_props(twenty_props))
    ylim(-0.01, 1.01)
    xlim(5, 14)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: do_savefig('20_aggregate_util.pdf')
    if not options.view_figs: close()
    
    figure(figsize=(4.5, 3)) #4.5, 3
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(fifty_util_btrelay_x, get_btrelay_props(fifty_props))
    do_plot_CDF(fifty_util_btclient_x, get_btclient_props(fifty_props))
    ylim(-0.01, 1.01)
    xlim(5, 14)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: do_savefig('50_aggregate_util.pdf')
    if not options.view_figs: close()

    figure(figsize=(4.5, 3)) #4.5, 3
    do_plot_CDF(tor_util_x, tor_props)
    do_plot_CDF(eighty_util_btrelay_x, get_btrelay_props(eighty_props))
    do_plot_CDF(eighty_util_btclient_x, get_btclient_props(eighty_props))
    ylim(-0.01, 1.01)
    xlim(5, 14)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Bandwidth Utilization (%)")
    do_legend()
    if options.save_figs: do_savefig('80_aggregate_util.pdf')
    if not options.view_figs: close()
    #show()
    #return
    print "graphing goodput"
    figure(figsize=(6, 4))
    do_plot_CDF(tor_client_good, tor_props)
    #do_plot_CDF(zero_util_btrelay_x, zero_props)
    do_plot_CDF(twenty_relay_good, twenty_props)
    do_plot_CDF(fifty_relay_good, fifty_props)
    do_plot_CDF(eighty_relay_good, eighty_props)
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Relay Goodput (kbps)")
    do_legend()
    if options.save_figs: do_savefig('btrelay_aggregate_good.pdf')
    if not options.view_figs: close()

    #util normal
    figure(figsize=(6, 4))
    do_plot_CDF(tor_client_good, tor_props)
    do_plot_CDF(zero_client_good, zero_props)
    do_plot_CDF(twenty_client_good, twenty_props)
    do_plot_CDF(fifty_client_good, fifty_props)
    do_plot_CDF(eighty_client_good, eighty_props)
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Client Goodput (kbps)")
    do_legend()
    if options.save_figs: do_savefig('btclient_aggregate_good.pdf')
    if not options.view_figs: close()

    figure(figsize=(6, 4)) #4.5, 3
    do_plot_CDF(tor_client_good, tor_props)
    do_plot_CDF(twenty_relay_good, get_btrelay_props(twenty_props))
    do_plot_CDF(twenty_client_good, get_btclient_props(twenty_props))
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Goodput (kbps)")
    do_legend()
    if options.save_figs: do_savefig('20_aggregate_goodput.pdf')
    if not options.view_figs: close()

    figure(figsize=(6, 4)) #4.5, 3
    do_plot_CDF(tor_client_good, tor_props)
    do_plot_CDF(fifty_relay_good, get_btrelay_props(fifty_props))
    do_plot_CDF(fifty_client_good, get_btclient_props(fifty_props))
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Goodput (kbps)")
    do_legend()
    if options.save_figs: do_savefig('50_aggregate_goodput.pdf')
    if not options.view_figs: close()

    figure(figsize=(6, 4)) #4.5, 3
    do_plot_CDF(tor_client_good, tor_props)
    do_plot_CDF(eighty_relay_good, get_btrelay_props(eighty_props))
    do_plot_CDF(eighty_client_good, get_btclient_props(eighty_props))
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Goodput (kbps)")
    do_legend()
    if options.save_figs: do_savefig('80_aggregate_goodput.pdf')
    if not options.view_figs: close()

    m = len(tor_util2_x)/num_markers
    mark_every_setting=(m,m)
    
    figure(figsize=(6, 4))
    do_plot_CDF(tor_util2_x, tor_props)
    do_plot_CDF(fifty_fsht_x, get_btht_props(fifty_props))
    do_plot_CDF(fifty_fsn_x, get_btn_props(fifty_props))
    ylim(0, 1)
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Goodput (kbps)")
    do_legend()
    if options.save_figs: do_savefig('50_util.pdf')
    if not options.view_figs: close()

    print "graphing messages, datagrams, and memory over time"
    # messages, datagrams, and memory over time
    figure(figsize=(6, 4))
    do_plot(time, tor_memory, tor_props)
    do_plot(time, fifty_memory, fifty_props)
    ylabel("Memory (MB)")
    xlabel("Time (m)")
    do_legend()
    if options.save_figs: do_savefig('memory.pdf')
    if not options.view_figs: close()

    figure(figsize=(6, 4))
    plot(time, tor_mgs_total, lw=line_width, label=tor_props[0])
    plot(time, fifty_mgs_total_y, lw=line_width, label=fifty_props[0] + " total")
    plot(time, fifty_mgs_ll_y, lw=line_width, label=fifty_props[0] + " LL")
    plot(time, fifty_mgs_ht_y, lw=line_width, label=fifty_props[0] + " HT")
    plot(time, fifty_mgs_n_y, lw=line_width, label=fifty_props[0] + " N")
    ylabel("Number of Messages")
    xlabel("Time (m)")
    do_legend()
    if options.save_figs: do_savefig('messages.pdf')
    if not options.view_figs: close()

    figure(figsize=(6, 4))
    plot(time, tor_dgs_total, lw=line_width, label=tor_props[0])
    plot(time, fifty_dgs_total_y, lw=line_width, label=fifty_props[0] + " total")
    plot(time, fifty_dgs_ll_y, lw=line_width, label=fifty_props[0] + " LL")
    plot(time, fifty_dgs_ht_y, lw=line_width, label=fifty_props[0] + " HT")
    plot(time, fifty_dgs_n_y, lw=line_width, label=fifty_props[0] + " N")
    ylabel("Number of Datagrams")
    xlabel("Time (m)")
    do_legend()
    if options.save_figs: do_savefig('datagrams.pdf')
    if not options.view_figs: close()

    numnodes = []
    for i in xrange(len(fifty_bwUp_y)):
        numnodes.append(i)
    figure(figsize=(6, 4))
#    plot(numnodes, fifty_bwDown_y, lw=line_width, color='b', label=fifty_props[0] + " Downstream")
    plot(numnodes, fifty_bwUp_y, lw=line_width, color='r', label=fifty_props[0] + " Upstream")
    plot(numnodes, fifty_bwCon_y, lw=line_width, color='g', label=fifty_props[0] + " Contributed")
#    fill_between(numnodes, fifty_bwDown_y, lw=line_width, color='b')
#    fill_between(numnodes, fifty_bwUp_y, lw=line_width, color='r')
#    fill_between(numnodes, fifty_bwCon_y, lw=line_width, color='g')
    ylabel("Bandwidth (kbps)")
    xlabel("Relays")
    do_legend()
    if options.save_figs: do_savefig('bandwidth.pdf')
    
    if options.view_figs: show()

def to_float(list):
    new = []
    for item in list:
        new.append(float(item))
    return new

def get_btht_props(props):
    l = get_btrelay_props(props)
    l[0] = props[0] + ", HT"
    return l
    
def get_btn_props(props):
    l = get_btclient_props(props)
    l[0] = props[0] + ", N"
    return l

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
    from numpy import arange
    do_plot(x, arange(len(x))/float(len(x)), props)
    
def do_plot(x, y, props):
    global mark_every_setting
    from matplotlib.pylab import plot
    ew = 1.0
    if props[3] == '+' or props[3] == 'x':
        ew = 1.5
    if mark_every_setting[1] == 0: mark_every_setting=(mark_every_setting[0],1)
    plot(x, y, markevery=mark_every_setting, ms=marker_size, mew=ew, lw=line_width, label=props[0], ls=props[1], c=props[2], marker=props[3])
    
def get_y_cdf_axis(x):
    y = []
    frac = 0
    for i in xrange(len(x)):
        frac += 1.0 / float(len(x))
        y.append(frac)
    return y



def do_legend():
    from matplotlib.pylab import legend
    # see http://matplotlib.sourceforge.net/users/legend_guide.html
    leg = legend(bbox_to_anchor=(0., 1.02, 1., .102), loc=3, ncol=5, mode="expand", borderaxespad=0., numpoints=1, handletextpad=0.2)
    if leg is not None:
        for t in leg.get_texts():
            t.set_fontsize('small')
    do_grid()
            
def do_legend2():
    from matplotlib.pylab import legend
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
    
def do_savefig(name):
    from matplotlib.pylab import savefig
    savefig(name, format='pdf')
            
def graph_all():
    if not options.view_figs:
        from matplotlib import use
        use('Agg')
    from matplotlib.pylab import figure, savefig, show, legend, plot, fill_between, xlabel, ylabel, xlim, ylim, gca, close, semilogx
    from numpy import arange

    # get all data
    web_client_n, web_client_ll, fileshare_relay_kbps, fileshare_relay_util, fileshare_client_kbps, fileshare_client_util = [],[],[],[],[],[]
    msg = []
    data = []
    time = []
    contributed_bw = []
    
    for i in range(len(args)):
        filename = args[i]
        if(filename.find("dat") > -1): file = open_log(filename)
        else: file = open_log(filename[:filename.rfind('.log')]+'.dat.gz')
        reader = csv.reader(file)
        #skip header
        header = reader.next()
        i = str(i)
        web_client_n.append((to_float(reader.next()), i))
        web_client_ll.append((to_float(reader.next()), i)) 
        fileshare_relay_kbps.append((to_float(reader.next()), i)) 
        fileshare_relay_util.append((to_float(reader.next()), i)) 
        fileshare_client_kbps.append((to_float(reader.next()), i)) 
        fileshare_client_util.append((to_float(reader.next()), i)) 
        time.append(to_float(reader.next()))
        reader.next()
        msg.append((to_float(reader.next()), i)) 
        reader.next()
        reader.next()
        reader.next()
        data.append((to_float(reader.next()), i)) 
        reader.next()
        reader.next()
        reader.next()
        reader.next()
        reader.next()
        contributed_bw.append((to_float(reader.next()), i))
        
        #wcn_x, wcll_x, fsrkbps_x, fsrutil_x, fsckbps_x, fscutil_x, totaltime, mem_y, msgt_y, msght_y, msgll_y, msgn_y, datt_y, datht_y, datll_y, datn_y, bwDown_y, bwUp_y, bwCon_y, fsht_x, fsn_x

    #now graph our data
    print "graphing lowlatency rtt"
    figure()
    for item in web_client_n:
        x = item[0]
        y = arange(len(x))/float(len(x))
        plot(x, y, lw=3, label=item[1])
    xlim(0,20)
    ylabel("Cumulative Fraction")
    xlabel("Unpaid Webpage Download Time (s)")
    do_legend2()
    if options.save_figs: do_savefig('n_rtt_all.pdf')
    if not options.view_figs: close()
    del(web_client_n)

    print "graphing normal rtt"
    figure()
    for item in web_client_ll:
        x = item[0]
        y = arange(len(x))/float(len(x))
        plot(x, y, lw=3, label=item[1])
    xlim(0,20)
    ylabel("Cumulative Fraction")
    xlabel("Paid Webpage Download Time (s)")
    do_legend2()
    if options.save_figs: do_savefig('ll_rtt_all.pdf')
    if not options.view_figs: close()
    del(web_client_ll)
    
    print "graphing relay util"
    figure()
    for item in fileshare_relay_kbps:
        x = item[0]
        y = arange(len(x))/float(len(x))
        plot(x, y, lw=3, label=item[1])
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Relay Goodput (kbps)")
    do_legend2()
    if options.save_figs: do_savefig('relay_goodput_all.pdf')
    if not options.view_figs: close()
    del(fileshare_relay_kbps)
    
    figure()
    for item in fileshare_relay_util:
        x = item[0]
        y = arange(len(x))/float(len(x))
        plot(x, y, lw=3, label=item[1])
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Relay Bandwidth Utilization (%)")
    do_legend2()
    if options.save_figs: do_savefig('relay_util_all.pdf')
    if not options.view_figs: close()
    del(fileshare_relay_util)
    
    print "graphing client util"
    figure()
    for item in fileshare_client_kbps:
        x = item[0]
        y = arange(len(x))/float(len(x))
        plot(x, y, lw=3, label=item[1])
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Relay Goodput (kbps)")
    do_legend2()
    if options.save_figs: do_savefig('client_goodput_all.pdf')
    if not options.view_figs: close()
    del(fileshare_client_kbps)
    
    figure()
    for item in fileshare_client_util:
        x = item[0]
        y = arange(len(x))/float(len(x))
        plot(x, y, lw=3, label=item[1])
    ylabel("Cumulative Fraction")
    xlabel("File Sharing Relay Bandwidth Utilization (%)")
    do_legend2()
    if options.save_figs: do_savefig('client_util_all.pdf')
    if not options.view_figs: close()
    del(fileshare_client_util)

    figure()
    for item in msg:
        plot(time[0], item[0], lw=3, label=item[1])
    ylabel("Number of Messages")
    xlabel("Time")
    do_legend2()
    if options.save_figs: do_savefig('msg_time.pdf')
    if not options.view_figs: close()
    del(msg)
    
    figure()
    for item in data:
        plot(time[0], item[0], lw=3, label=item[1])
    ylabel("Number of Datagrams")
    xlabel("Time")
    do_legend2()
    if options.save_figs: do_savefig('data_time.pdf')
    if not options.view_figs: close()
    del(data)
    del(time)
    
    figure()
    item = contributed_bw[0]
    semilogx(item[0], arange(len(item[0]))/float(len(item[0])), lw=3)#, label=item[1])
    ylabel("Cumulative Fraction")
    xlabel("Bandwidth (kbps)")
    do_legend2()
    if options.save_figs: do_savefig('contributed_bandwidth.pdf')
    if not options.view_figs: close()
    del(contributed_bw)
    
    if options.view_figs: show()       

if __name__ == '__main__':
    #draw_relay_bandwidth_CDF()
    if options.do_parse is True:
        print "Parsing files..."
        if options.do_parallel: parallel_parse()
        else: parse_all_args()
    if options.do_graph is True:
        print "Graphing csv data..."
        if options.graph_all: graph_all()
        else: graph_from_csv()
    print "done!"
        