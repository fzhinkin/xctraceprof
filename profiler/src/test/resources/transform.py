import xml.dom.minidom as minidom
import sys

# Convert a xml file with an exported profile to csv file.
# Usage: python3 <xml file> <csv file>

cache = {}
document = minidom.parse(sys.argv[1])
rows = document.getElementsByTagName("row")

def scanAndCache(element):
    if element.nodeType == 3:
        return
    if element.hasAttribute("ref"):
        return
    for idx in range(0, len(element.childNodes)):
        node = element.childNodes[idx]
        if node.nodeType == 3:
            continue

        if node.hasAttribute("ref"):
            element.childNodes[idx] = cache[node.getAttribute("ref")]
        else:
            scanAndCache(node)

    if element.hasAttribute("id"):
        cache[element.getAttribute("id")] = element

def row2csv(row):
    time = row.getElementsByTagName("sample-time")[0].childNodes[0].nodeValue
    weight = 0
    for tag in ["cycle-weight", "weight", "pmc-event"]:
        weight_el = row.getElementsByTagName(tag)
        if len(weight_el) == 0:
            continue
        weight = weight_el[0].childNodes[0].nodeValue
        break
    backtrace_nodes = row.getElementsByTagName("backtrace")
    lib = ''
    addr = '0x0'
    sym = ''
    if len(backtrace_nodes) == 1:
        backtrace = backtrace_nodes[0]
        top_frame = backtrace.childNodes[0]
        binaryNode = top_frame.getElementsByTagName("binary")
        if len(binaryNode) == 1:
            lib = binaryNode[0].getAttribute("name")
        addr = top_frame.getAttribute("addr")
        sym = top_frame.getAttribute("name")
    events_nodes = row.getElementsByTagName("pmc-events")
    events = ''
    if len(events_nodes) == 1:
        events = events_nodes[0].childNodes[0].nodeValue

    csv_row = "%s;%s;%s;%s;%s;%s" % (time, weight, addr, sym, lib, events)
    return csv_row

with open(sys.argv[2], 'w') as out:
    for row in rows:
        scanAndCache(row)
        out.write(row2csv(row))
        out.write('\n')
