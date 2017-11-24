#!/usr/bin/env python
import sqlite3

def main():
	conn = sqlite3.connect('A3.db')
	c = conn.cursor()

	outfile = open('accel.svm', 'wb')

	def label_to_id(s):
		if s == u'Walking': return -1
		if s == u'Running': return  0
		if s == u'Jumping': return  1
		assert(False)

	for data in c.execute('SELECT * FROM `samples`'):
		row_id = data[0]
		label = data[-1]
		data = data[1:-2]
		s = '%d ' % label_to_id(label)
		for i, v in enumerate(data):
			s += '%d:%f ' % (i+1,v/10.0) # Scale by a factor of 0.1
		s += '\n'
		outfile.write(s)

if __name__ == '__main__':
	main()