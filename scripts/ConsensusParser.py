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

$id$
'''
#!/usr/bin/python

bw = []

for line in open("NetworkConsensus_1-12-10.txt"):
	if line.find("Bandwidth") > -1:
		t = line[line.find('=')+1:]
		bw.append(int(t.strip()))

bw.sort()
for item in bw:
	print item

