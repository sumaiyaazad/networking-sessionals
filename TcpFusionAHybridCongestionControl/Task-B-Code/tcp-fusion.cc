/* -*- Mode:C++; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/*
 * Copyright (c) 2016 ResiliNets, ITTC, University of Kansas
 *
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
 *
 * Author: Truc Anh N. Nguyen <annguyen@ittc.ku.edu>
 *
 * James P.G. Sterbenz <jpgs@ittc.ku.edu>, director
 * ResiliNets Research Group  http://wiki.ittc.ku.edu/resilinets
 * Information and Telecommunication Technology Center (ITTC)
 * and Department of Electrical Engineering and Computer Science
 * The University of Kansas Lawrence, KS USA.
 */

#include "tcp-fusion.h"
#include "tcp-socket-state.h"
#include "ns3/simulator.h"

#include "ns3/log.h"

namespace ns3 {

NS_LOG_COMPONENT_DEFINE ("TcpFusion");
NS_OBJECT_ENSURE_REGISTERED (TcpFusion);

TypeId
TcpFusion::GetTypeId (void)
{
  static TypeId tid = TypeId ("ns3::TcpFusion")
    .SetParent<TcpNewReno> ()
    .AddConstructor<TcpFusion> ()
    .SetGroupName ("Internet")
    .AddTraceSource("EstimatedBW", "The estimated bandwidth",
                    MakeTraceSourceAccessor(&TcpFusion::m_currentBW),
                    "ns3::TracedValueCallback::Double")
  ;
  return tid;
}

TcpFusion::TcpFusion (void)
  : TcpNewReno (),
    m_currentBW (0),
    m_lastSampleBW (0),
    m_lastBW (0),
    m_ackedSegments (0),
    m_IsCount (false),
    m_lastAck (0),
    m_alpha (0),
    m_diff (0),
    m_wInc (0),
    m_baseRtt (Time::Max ()),
    m_minRtt (Time::Max ()),
    m_currentRtt (Time::Max()),
    m_cntRtt (0),
    m_doingFusionNow (true),
    m_begSndNxt (0)
{
  NS_LOG_FUNCTION (this);
}

TcpFusion::TcpFusion (const TcpFusion& sock)
  : TcpNewReno (sock),
    m_currentBW (sock.m_currentBW),
    m_lastSampleBW (sock.m_lastSampleBW),
    m_lastBW (sock.m_lastBW),
    m_IsCount (sock.m_IsCount),
    m_alpha (sock.m_alpha),
    m_diff (sock.m_diff),
    m_wInc (sock.m_wInc),
    m_baseRtt (sock.m_baseRtt),
    m_minRtt (sock.m_minRtt),
    m_currentRtt (sock.m_currentRtt),
    m_cntRtt (sock.m_cntRtt),
    m_doingFusionNow (true),
    m_begSndNxt (0)
{
  NS_LOG_FUNCTION (this);
}

TcpFusion::~TcpFusion (void)
{
  NS_LOG_FUNCTION (this);
}

Ptr<TcpCongestionOps>
TcpFusion::Fork (void)
{
  return CopyObject<TcpFusion> (this);
}


void
TcpFusion::PktsAcked (Ptr<TcpSocketState> tcb, uint32_t segmentsAcked,
                     const Time& rtt)
{
  NS_LOG_FUNCTION (this << tcb << segmentsAcked << rtt);

  if (rtt.IsZero ())
    {
      return;
    }
  m_ackedSegments += segmentsAcked;
  EstimateBW (rtt, tcb);
 
  m_currentRtt = rtt;
  // std::cout << " seconds : " << m_currentRtt.GetSeconds () << "\n";

  m_minRtt = std::min (m_minRtt, rtt);
  NS_LOG_DEBUG ("Updated m_minRtt = " << m_minRtt);

  m_baseRtt = std::min (m_baseRtt, rtt);
  NS_LOG_DEBUG ("Updated m_baseRtt = " << m_baseRtt);

  // Update RTT counter
  m_cntRtt++;
  NS_LOG_DEBUG ("Updated m_cntRtt = " << m_cntRtt);
}

void
TcpFusion::EstimateBW (const Time &rtt, Ptr<TcpSocketState> tcb)
{
  NS_LOG_FUNCTION (this);

  NS_ASSERT (!rtt.IsZero ());

  m_currentBW = m_ackedSegments * tcb->m_segmentSize / rtt.GetSeconds ();


  Time currentAck = Simulator::Now ();
  m_currentBW = m_ackedSegments * tcb->m_segmentSize / (currentAck - m_lastAck).GetSeconds ();
  m_lastAck = currentAck;

  m_ackedSegments = 0;
  NS_LOG_LOGIC ("Estimated BW: " << m_currentBW);

  // Filter the BW sample
  NS_LOG_LOGIC ("Estimated BW after filtering: " << m_currentBW);
}


void
TcpFusion::IncreaseWindow (Ptr<TcpSocketState> tcb, uint32_t segmentsAcked)
{
  // std::cout << " < reno window\n";
    if (tcb->m_cWnd < tcb->m_ssThresh)
    {
      // std::cout << "tcb->m_cWnd < tcb->m_ssThresh\n";
      // std::cout <<Simulator::Now () <<" prev congestion window : "<<tcb->m_cWnd.Get ()<<"   ";
      segmentsAcked = TcpNewReno::SlowStart (tcb, segmentsAcked);
      // std::cout << "congestion window : "<<tcb->m_cWnd.Get ()<<"\n";
    }
    if (tcb->m_cWnd >= tcb->m_ssThresh)
    { uint32_t  reno_cWnd = tcb->m_cWnd;
      std::cout << "tcb->m_cWnd >= tcb->m_ssThresh\n";
      if (m_cntRtt < 3)
      {    
        std::cout << "m_cntRtt < 3\n"; 
        std::cout <<Simulator::Now () <<"  prev congestion window : "<<tcb->m_cWnd.Get ()<<"   ";
        TcpNewReno::IncreaseWindow (tcb, segmentsAcked);
        std::cout << "congestion window : "<<tcb->m_cWnd.Get ()<<"\n";
      }
      else if(m_currentRtt.GetSeconds ()>2*m_minRtt.GetSeconds ()){
        std::cout << "m_currentRtt.GetSeconds ()>2*m_minRtt.GetSeconds ()\n";
        std::cout <<Simulator::Now () <<"  prev congestion window : "<<tcb->m_cWnd.Get ()<<"   ";
        tcb->m_cWnd = std::max ((m_minRtt.GetSeconds ()/m_currentRtt.GetSeconds ()), 0.5) * tcb->m_cWnd.Get ();
        std::cout << "congestion window : "<<tcb->m_cWnd.Get ()<<"\n";
      }
      else{
        std::cout << "else\n";
        m_wInc=(m_currentBW)/(8*1500);
        m_alpha=(tcb->m_cWnd.Get ()*(.004))/m_currentRtt.GetSeconds ();
        m_diff=(tcb->m_cWnd.Get ()*(m_currentRtt.GetSeconds ()-m_minRtt.GetSeconds ()))/m_currentRtt.GetSeconds ();
        if(m_diff<m_alpha){
          std::cout << "diff < alpha \n";
          std::cout <<Simulator::Now () <<"  prev congestion window : "<<tcb->m_cWnd.Get ()<<"   ";
          tcb->m_cWnd = tcb->m_cWnd.Get () + (m_wInc)*tcb->m_segmentSize;
          std::cout << "congestion window : "<<tcb->m_cWnd.Get ()<<"\n";
        }else if(m_diff>3*m_alpha){
          std::cout << "diff > alpha \n";
          std::cout <<Simulator::Now () <<"  prev congestion window : "<<tcb->m_cWnd.Get ()<<"   ";
          tcb->m_cWnd = tcb->m_cWnd.Get () + ((-m_diff+m_alpha)/tcb->m_cWnd.Get ())*tcb->m_segmentSize; 
          std::cout << "congestion window : "<<tcb->m_cWnd.Get ()<<"\n";
        }else{
          std::cout << "nothing to do \n";
          std::cout << Simulator::Now () <<"  prev congestion window : "<<tcb->m_cWnd.Get ()<<"   ";
          std::cout << "congestion window : "<<tcb->m_cWnd.Get ()<<"\n";
        }
        if (segmentsAcked > 0)
        {
          double adder = static_cast<double> (tcb->m_segmentSize * tcb->m_segmentSize) / tcb->m_cWnd.Get ();
          adder = std::max (1.0, adder);
          reno_cWnd += static_cast<uint32_t> (adder);
        }
        if(tcb->m_cWnd<reno_cWnd){
          std::cout << "tcb->m_cWnd<reno_cWnd\n";
          std::cout <<Simulator::Now ()<< " prev congestion window : "<<tcb->m_cWnd.Get ()<<"   ";
          tcb->m_cWnd = reno_cWnd;
          std::cout << "congestion window : "<<tcb->m_cWnd.Get ()<<"\n";
        }
      }
    }
}

std::string
TcpFusion::GetName () const
{
  return "TcpFusion";
}

uint32_t
TcpFusion::GetSsThresh (Ptr<const TcpSocketState> tcb,
                       uint32_t bytesInFlight)
{
  NS_LOG_FUNCTION (this << tcb << bytesInFlight);
  return std::max (std::min (tcb->m_ssThresh.Get (), tcb->m_cWnd.Get () - tcb->m_segmentSize), 2 * tcb->m_segmentSize);
}

} // namespace ns3
