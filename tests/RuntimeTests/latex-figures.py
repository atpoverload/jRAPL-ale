from sys import argv
import os
'''------------------------------------'''
def print_image_template(gp):
	domain=''
	subdomain=''
	system=''

	system = 'SystemC'
	if '/CFunctions/' in gp:
		domain = 'C-Functions'
	if '/JavaFunctions/' in gp:
		domain = 'Java-Functions'
	if '/PerSocketMSRReadings/' in gp:
		domain = 'Per-Socket-MSR-Readings'
	if 'profileInit' in gp:
		subdomain='profileInit'
	if 'energyStatCheck' in gp:
		subdomain='energyStatCheck'
	if 'profileDealloc' in gp:
		subdomain = 'profileDealloc'
	if 'PACKAGE' in gp:
		subdomain = 'PACKAGE'
	if 'CORE' in gp:
		subdomain = 'CORE'
	if 'DRAM' in gp:
		subdomain = 'DRAM'
	if 'scatter' in gp:
		caption = 'Scatter plot of ' + domain + ' runtime for ' + subdomain + ' for ' + system
	if 'bar_graph' in gp:
		caption = 'Bar graph comparing Java and C function runtime'
		domain = 'bar-graph'
	'''else:
		return None'''

	filename=gp
	#caption='cp for ' + system
	label=domain+'|'+subdomain+'|'+system
	print('\\begin{figure}[H]\n\t\\centering\n\t\\includegraphics[width=10cm,height=10cm,keepaspectratio]{'+filename+'}\n\t\\caption{'+caption+'}\n\t\\label{fig:'+label+'}\n\\end{figure}\n')


'''------------------------------------'''
fh=open(argv[1],'r')
graphpaths=fh.readlines()
fh.close()

for gp in graphpaths:
	gp=gp.strip()
	print_image_template(gp)

