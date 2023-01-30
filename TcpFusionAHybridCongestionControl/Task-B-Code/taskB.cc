/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <fstream>
#include "ns3/core-module.h"
#include "ns3/network-module.h"
#include "ns3/internet-module.h"
#include "ns3/point-to-point-module.h"
#include "ns3/applications-module.h"
#include "ns3/stats-module.h"
#include "ns3/csma-module.h"
#include "ns3/mobility-module.h"
#include "ns3/packet-sink.h"
#include "ns3/packet-sink-helper.h"
#include "ns3/flow-monitor-helper.h"
#include "ns3/flow-monitor-module.h"
#include "ns3/ipv4-address-helper.h"
#include "ns3/ipv4-global-routing-helper.h"
#include "ns3/internet-stack-helper.h"

using namespace ns3;

NS_LOG_COMPONENT_DEFINE ("SeventhScriptExample");

// Default Network Topology
//
//   csma 10.1.3.0
                
//  *    *    *    *
//  |    |    |    |    10.1.1.0
//  n6  n7   n8   n9   n0 ------------------ n1   n2   n3   n4  n5
//                       point-to-point      |    |    |    |   |
//                                           *    *    *    *   *
//                                              csma 10.1.2.0
//
//
// We want to look at changes in the ns-3 TCP congestion window.  We need
// to crank up a flow and hook the CongestionWindow attribute on the soc
// of the sender.  Normally one would use an on-off application to generate a
// flow, but this has a couple of problems.  First, the socket of the on-off
// application is not created until Application Start time, so we wouldn't be
// able to hook the socket (now) at configuration time.  Second, even if we
// could arrange a call after start time, the socket is not public so we
// couldn't get at it.
//
// So, we can cook up a simple version of the on-off application that does what
// we want.  On the plus side we don't need all of the complexity of the on-off
// application.  On the minus side, we don't have a helper, so we have to get
// a little more involved in the details, but this is trivial.
//
// So first, we create a socket and do the trace connect on it; then we pass
// this socket into the constructor of our simple application which we then
// install in the source node.
//
// NOTE: If this example gets modified, do not forget to update the .png figure
// in src/stats/docs/seventh-packet-byte-count.png
// ===========================================================================
//
class MyApp : public Application
{
public:
  MyApp ();
  virtual ~MyApp ();

  /**
   * Register this type.
   * \return The TypeId.
   */
  static TypeId GetTypeId (void);
  void Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, uint32_t nPackets, DataRate dataRate);

private:
  virtual void StartApplication (void);
  virtual void StopApplication (void);

  void ScheduleTx (void);
  void SendPacket (void);

  Ptr<Socket>     m_socket;
  Address         m_peer;
  uint32_t        m_packetSize;
  uint32_t        m_nPackets;
  DataRate        m_dataRate;
  EventId         m_sendEvent;
  bool            m_running;
  uint32_t        m_packetsSent;
};

MyApp::MyApp ()
  : m_socket (0),
    m_peer (),
    m_packetSize (0),
    m_nPackets (0),
    m_dataRate (0),
    m_sendEvent (),
    m_running (false),
    m_packetsSent (0)
{
}

MyApp::~MyApp ()
{
  m_socket = 0;
}

/* static */
TypeId MyApp::GetTypeId (void)
{
  static TypeId tid = TypeId ("MyApp")
    .SetParent<Application> ()
    .SetGroupName ("Tutorial")
    .AddConstructor<MyApp> ()
    ;
  return tid;
}

void
MyApp::Setup (Ptr<Socket> socket, Address address, uint32_t packetSize, uint32_t nPackets, DataRate dataRate)
{
  m_socket = socket;
  m_peer = address;
  m_packetSize = packetSize;
  m_nPackets = nPackets;
  m_dataRate = dataRate;
}

void
MyApp::StartApplication (void)
{
  m_running = true;
  m_packetsSent = 0;
  if (InetSocketAddress::IsMatchingType (m_peer))
    {
      m_socket->Bind ();
    }
  else
    {
      m_socket->Bind6 ();
    }
  m_socket->Connect (m_peer);
  SendPacket ();
}

void
MyApp::StopApplication (void)
{
  m_running = false;

  if (m_sendEvent.IsRunning ())
    {
      Simulator::Cancel (m_sendEvent);
    }

  if (m_socket)
    {
      m_socket->Close ();
    }
}

void
MyApp::SendPacket (void)
{
  Ptr<Packet> packet = Create<Packet> (m_packetSize);
  m_socket->Send (packet);

  if (++m_packetsSent < m_nPackets)
    {
      ScheduleTx ();
    }
}

void
MyApp::ScheduleTx (void)
{
  if (m_running)
    {
      Time tNext (Seconds (m_packetSize * 8 / static_cast<double> (m_dataRate.GetBitRate ())));
      m_sendEvent = Simulator::Schedule (tNext, &MyApp::SendPacket, this);
    }
}

static void
CwndChange (Ptr<OutputStreamWrapper> stream, uint32_t oldCwnd, uint32_t newCwnd)
{
  // NS_LOG_UNCOND (Simulator::Now ().GetSeconds () << "\t" << newCwnd);
  *stream->GetStream () << Simulator::Now ().GetSeconds () << "\t" << oldCwnd << "\t" << newCwnd << std::endl;
}

uint64_t totalPacketsSent= 0; 
uint64_t totalPacketsDropped= 0;  

static void
RxDrop (Ptr<OutputStreamWrapper> stream, Ptr<const Packet> p)
{
  totalPacketsDropped+=1;
}
static void TxEnd(Ptr<OutputStreamWrapper> stream, Ptr<const Packet> p) 
{
  totalPacketsSent +=1;
}

AsciiTraceHelper asciiTraceHelper;
Ptr<OutputStreamWrapper> streamDropRatio = asciiTraceHelper.CreateFileStream("dropRatio-wire");
void
CalculateDropRatio ()
{
  Time now = Simulator::Now ();                                         /* Return the simulator's virtual time. */
  double cur = (totalPacketsDropped) * (double) 1 / (totalPacketsSent);     /* Convert Application RX Packets to MBits. */
  *streamDropRatio->GetStream () << now.GetSeconds () << "  " << cur << std::endl;
  totalPacketsDropped=0;
  totalPacketsSent=0;
  Simulator::Schedule (MilliSeconds (100), &CalculateDropRatio);
}

Ptr<PacketSink> sink;                         /* Pointer to the packet sink application */
uint64_t lastTotalRx = 0;  
Ptr<OutputStreamWrapper> streamThroughput = asciiTraceHelper.CreateFileStream("throughput-wire");
void
CalculateThroughput ()
{
  Time now = Simulator::Now ();                                         /* Return the simulator's virtual time. */
  double cur = (sink->GetTotalRx () - lastTotalRx) * (double) 8 / 1e5;     /* Convert Application RX Packets to MBits. */
  *streamThroughput->GetStream () << now.GetSeconds () << "  " << cur << std::endl;
  lastTotalRx = sink->GetTotalRx ();
  Simulator::Schedule (MilliSeconds (100), &CalculateThroughput);
}

int
main (int argc, char *argv[])
{
  bool useV6 = false;
  std::string tcpVariant = "TcpFusion"; 
  tcpVariant = std::string ("ns3::") + tcpVariant;
  Config::SetDefault ("ns3::TcpL4Protocol::SocketType", TypeIdValue (TypeId::LookupByName (tcpVariant)));

  uint32_t nCsma = 5;
  CommandLine cmd (__FILE__);
  cmd.AddValue ("useIpv6", "Use Ipv6", useV6);
  cmd.Parse (argc, argv);

  NodeContainer nodes; // p2pNodes
  nodes.Create (2);

  PointToPointHelper pointToPoint;
  pointToPoint.SetDeviceAttribute ("DataRate", StringValue ("50000Mbps"));
  // pointToPoint.SetChannelAttribute ("Delay", StringValue ("10ms"));
  pointToPoint.SetChannelAttribute ("Delay", StringValue ("600ns"));


  NetDeviceContainer devices;
  devices = pointToPoint.Install (nodes);

  NodeContainer csmaNodes;
  csmaNodes.Add (nodes.Get (1));
  csmaNodes.Create (nCsma);

  NodeContainer csmaNodes2;
  csmaNodes2.Add (nodes.Get (0));
  csmaNodes2.Create (nCsma);

  CsmaHelper csma;
  csma.SetChannelAttribute ("DataRate", StringValue ("50000Mbps"));
  csma.SetChannelAttribute ("Delay", TimeValue (NanoSeconds (6560)));
  // csma.SetChannelAttribute ("Delay", TimeValue (NanoSeconds (600)));

  NetDeviceContainer csmaDevices;
  csmaDevices = csma.Install (csmaNodes);
  NetDeviceContainer csmaDevices2;
  csmaDevices2 = csma.Install (csmaNodes2);
  

  Ptr<RateErrorModel> em = CreateObject<RateErrorModel> ();
  em->SetAttribute ("ErrorRate", DoubleValue (0.001));

  InternetStackHelper stack;
  stack.Install (csmaNodes2);
  stack.Install (csmaNodes);

  uint16_t sinkPort = 8080;
  Address sinkAddress;
  Address anyAddress;
  std::string probeType;
  std::string tracePath;
  Ipv4AddressHelper address;

  address.SetBase ("10.1.1.0", "255.255.255.0");
  Ipv4InterfaceContainer p2pInterfaces;
  p2pInterfaces = address.Assign (devices);
  
  probeType = "ns3::Ipv4PacketProbe";
  tracePath = "/NodeList/*/$ns3::Ipv4L3Protocol/Tx";

  address.SetBase ("10.1.2.0", "255.255.255.0");
  Ipv4InterfaceContainer csmaInterfaces;
  csmaInterfaces = address.Assign (csmaDevices);

  address.SetBase ("10.1.3.0", "255.255.255.0");
  Ipv4InterfaceContainer csmaInterfaces2;
  csmaInterfaces2 = address.Assign (csmaDevices2);

  int num_half_flows = 4;
  Ptr<Socket> ns3TcpSocket;
  Ipv4GlobalRoutingHelper::PopulateRoutingTables();
  uint32_t payloadSize = 5040;           /* Transport layer payload size in bytes. */
  uint32_t rate = (payloadSize * 100 * 8) / 1000000; 
  std::ostringstream rateString;
  rateString << rate << "Mbps";
  std::string dataRate = rateString.str();  
  for(int i = 1; i <= num_half_flows; i++) {
    csmaDevices.Get (i)->SetAttribute ("ReceiveErrorModel", PointerValue (em));
    sinkAddress = InetSocketAddress (csmaInterfaces.GetAddress (i), sinkPort);
    anyAddress = InetSocketAddress (Ipv4Address::GetAny (), sinkPort);
    sinkPort++;
    PacketSinkHelper packetSinkHelper ("ns3::TcpSocketFactory", anyAddress);
    ApplicationContainer sinkApps = packetSinkHelper.Install (csmaNodes.Get (i));
    sink = StaticCast<PacketSink> (sinkApps.Get (0));
    sinkApps.Start (Seconds (0));
    // sinkApps.Stop (Seconds (i*2+20));
    ns3TcpSocket = Socket::CreateSocket (csmaNodes2.Get (i), TcpSocketFactory::GetTypeId ());
    Ptr<MyApp> app = CreateObject<MyApp> ();
    app->Setup (ns3TcpSocket, sinkAddress, 1040, 5000, DataRate ("50Mbps"));
    csmaNodes2.Get (i)->AddApplication (app);
    app->SetStartTime (Seconds (0.5+i*0.5));
    std::ostringstream oss;
    oss << "./LowRate/flow" << i  << ".cwnd";
    Ptr<OutputStreamWrapper> stream = asciiTraceHelper.CreateFileStream(oss.str());
    ns3TcpSocket->TraceConnectWithoutContext("CongestionWindow", MakeBoundCallback(&CwndChange, stream));
  }

  AsciiTraceHelper asciiTraceHelper;
  Ptr<OutputStreamWrapper> stream = asciiTraceHelper.CreateFileStream ("congestion-wire");
  ns3TcpSocket->TraceConnectWithoutContext ("CongestionWindow", MakeBoundCallback (&CwndChange, stream));
  Ptr<OutputStreamWrapper> streamRx = asciiTraceHelper.CreateFileStream("rx");
  csmaDevices.Get (1)->TraceConnectWithoutContext ("PhyRxDrop", MakeBoundCallback (&RxDrop, streamRx));
  Ptr<OutputStreamWrapper> streamTx = asciiTraceHelper.CreateFileStream("tx");
  csmaDevices2.Get (1)->TraceConnectWithoutContext ("PhyTxEnd", MakeBoundCallback (&TxEnd, streamTx));

  Ptr<FlowMonitor> flowMonitor;
  FlowMonitorHelper flowHelper;
  flowMonitor=flowHelper.InstallAll();
  flowMonitor->CheckForLostPackets ();
  Ptr<Ipv4FlowClassifier> classifier = DynamicCast<Ipv4FlowClassifier> (flowHelper.GetClassifier ());
  std::map<FlowId, FlowMonitor::FlowStats> stats = flowMonitor->GetFlowStats ();
  double simulationTime = 3;
  Simulator::Schedule (Seconds (1.1), &CalculateThroughput);
  Simulator::Schedule (Seconds (1.1), &CalculateDropRatio);
  Simulator::Stop (Seconds (simulationTime));
  Simulator::Run ();
  Simulator::Destroy ();
  flowMonitor->SerializeToXmlFile("flowmonitor-wire",false,false);
  double averageThroughput = ((sink->GetTotalRx () * 8) / (1e6 * simulationTime));
  std::cout << "\nAverage throughput: " << averageThroughput << " Mbit/s" << std::endl;
  return 0;
}

